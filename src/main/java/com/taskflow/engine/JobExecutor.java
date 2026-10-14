package com.taskflow.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.execution.ExecutionStatus;
import com.taskflow.execution.JobExecution;
import com.taskflow.execution.JobExecutionRepository;
import com.taskflow.execution.JobLogLevel;
import com.taskflow.execution.JobLogRepository;
import com.taskflow.job.BackoffPolicy;
import com.taskflow.job.JobRepository;
import com.taskflow.job.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The heart of the execution engine:
 *
 *  1. claimOne() runs in a SHORT transaction: one row is selected with
 *     FOR UPDATE SKIP LOCKED, marked RUNNING with a lease, the attempt
 *     counter is incremented and the execution record created. The DB lock
 *     is released at commit; nothing is executed while holding it.
 *  2. run() executes the handler OUTSIDE any transaction, renews the lease on
 *     a heartbeat schedule, enforces the job timeout, and polls the
 *     cancellation flag from the DB. It only computes an ExecOutcome - no
 *     durable writes, so a crash mid-run is repaired by the lease reaper.
 *  3. finalizeSuccess/finalizeError/finalizeCancelled each run in a SEPARATE
 *     short transaction (called through the Spring proxy) and move the job to
 *     SUCCEEDED / RETRY_WAIT / DEAD_LETTER / CANCELLED / next occurrence.
 */
@Component
public class JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(JobExecutor.class);
    private static final long CANCEL_CHECK_MS = 2_000;

    private final JobRepository jobs;
    private final JobExecutionRepository executions;
    private final JobLogRepository logs;
    private final HandlersRegistry registry;
    private final ObjectMapper mapper;
    private final EngineProperties props;

    public JobExecutor(JobRepository jobs, JobExecutionRepository executions, JobLogRepository logs,
                       HandlersRegistry registry, ObjectMapper mapper, EngineProperties props) {
        this.jobs = jobs;
        this.executions = executions;
        this.logs = logs;
        this.registry = registry;
        this.mapper = mapper;
        this.props = props;
    }

    @Transactional
    public Optional<ClaimedJob> claimOne(String workerName) {
        Optional<UUID> claimable = jobs.findClaimableJobId();
        if (claimable.isEmpty()) {
            return Optional.empty();
        }
        UUID jobId = claimable.get();
        OffsetDateTime leaseUntil = OffsetDateTime.now().plusSeconds(props.worker().leaseSeconds());
        if (jobs.claimJob(jobId, workerName, leaseUntil) == 0) {
            return Optional.empty(); // lost a race
        }
        // Read inside the same transaction while the row lock is still held
        JobEntitySnapshot snapshot = JobEntitySnapshot.of(jobs.findById(jobId).orElseThrow());
        executions.save(new JobExecution(snapshot.executionId(), jobs.getReferenceById(jobId),
                snapshot.attemptNumber()));
        logEvent(jobId, snapshot.executionId(), JobLogLevel.INFO,
                "Claimed by " + workerName + " (attempt " + snapshot.attemptNumber() + ")");
        return Optional.of(snapshot.toClaimedJob(workerName, jobId));
    }

    /** Executes the claimed job off-transaction and returns the raw outcome. */
    public ExecOutcome run(ClaimedJob claimed) {
        long startNanos = System.nanoTime();
        var handler = registry.require(claimed.jobType().name().toLowerCase());

        com.fasterxml.jackson.databind.JsonNode payload;
        try {
            payload = claimed.payloadJson() == null
                    ? mapper.nullNode()
                    : mapper.readTree(claimed.payloadJson());
        } catch (Exception e) {
            return ExecOutcome.failure("PAYLOAD_INVALID",
                    "Payload is not valid JSON: " + safeMessage(e.getMessage()), 0L);
        }

        AtomicBoolean cancelled = new AtomicBoolean(false);
        var ctx = new JobHandler.ExecutionContext(claimed.jobId(), claimed.attemptNumber(), cancelled::get);

        var pool = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "job-exec-" + claimed.jobId());
            t.setDaemon(true);
            return t;
        });
        try {
            var future = pool.submit(() -> handler.execute(payload, ctx));
            long deadlineNanos = startNanos + claimed.timeoutSeconds() * 1_000_000_000L;
            long nextLeaseRenewMs = 0L;
            long nextCancelCheckMs = 0L;
            while (true) {
                long nowMs = System.currentTimeMillis();
                if (nowMs >= nextLeaseRenewMs) {
                    renewLeaseQuietly(claimed);
                    nextLeaseRenewMs = nowMs + props.worker().heartbeatSeconds() * 1000L;
                }
                if (nowMs >= nextCancelCheckMs) {
                    cancelled.set(jobs.isCancelRequestedRaw(claimed.jobId()));
                    nextCancelCheckMs = nowMs + CANCEL_CHECK_MS;
                }
                if (cancelled.get()) {
                    future.cancel(true);
                    return ExecOutcome.cancelled(elapsedMs(startNanos));
                }
                if (System.nanoTime() >= deadlineNanos) {
                    future.cancel(true);
                    return ExecOutcome.failure("TIMEOUT",
                            "Job exceeded its timeout of " + claimed.timeoutSeconds() + "s",
                            elapsedMs(startNanos));
                }
                try {
                    var result = future.get(250, TimeUnit.MILLISECONDS);
                    return ExecOutcome.success(result, elapsedMs(startNanos));
                } catch (java.util.concurrent.TimeoutException expected) {
                    // no result yet; keep polling
                } catch (java.util.concurrent.ExecutionException e) {
                    var cause = e.getCause() == null ? e : e.getCause();
                    return ExecOutcome.failure("HANDLER_ERROR", safeMessage(cause.getMessage()),
                            elapsedMs(startNanos));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return ExecOutcome.failure("INTERRUPTED", "Worker thread interrupted",
                            elapsedMs(startNanos));
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }

    void renewLease(ClaimedJob claimed) {
        OffsetDateTime leaseUntil = OffsetDateTime.now().plusSeconds(props.worker().leaseSeconds());
        jobs.renewLease(claimed.jobId(), claimed.workerName(), leaseUntil);
    }

    private void renewLeaseQuietly(ClaimedJob claimed) {
        try {
            renewLease(claimed);
        } catch (Exception e) {
            log.warn("Lease renewal failed for job {}: {}", claimed.jobId(), e.getMessage());
        }
    }

    @Transactional
    public void finalizeSuccess(ClaimedJob claimed, ExecOutcome outcome) {
        closeExecution(claimed, ExecutionStatus.SUCCEEDED, outcome.durationMs(), outcome.result(), null, null);
        if (claimed.recurringCron() != null) {
            OffsetDateTime nextRun = nextCronRun(claimed.recurringCron());
            jobs.completeAttempt(claimed.jobId(), claimed.workerName(), JobStatus.QUEUED, nextRun);
            jobs.resetAttemptsAfterRequeue(claimed.jobId());
            logEvent(claimed.jobId(), claimed.executionId(), JobLogLevel.INFO,
                    "Attempt " + claimed.attemptNumber() + " succeeded in " + outcome.durationMs()
                            + "ms; recurring job requeued for " + nextRun);
        } else {
            int rows = jobs.completeAttempt(claimed.jobId(), claimed.workerName(), JobStatus.SUCCEEDED,
                    OffsetDateTime.now());
            logEvent(claimed.jobId(), claimed.executionId(), JobLogLevel.INFO,
                    "Attempt " + claimed.attemptNumber() + " succeeded in " + outcome.durationMs() + "ms");
            if (rows == 0) {
                log.warn("Success finalize raced for job {}", claimed.jobId());
            }
        }
    }

    @Transactional
    public void finalizeError(ClaimedJob claimed, ExecOutcome outcome) {
        closeExecution(claimed, ExecutionStatus.FAILED, outcome.durationMs(), null,
                outcome.errorCode(), outcome.errorMessage());
        if (claimed.recurringCron() != null) {
            // Recurring jobs keep their beat even when a single run fails
            OffsetDateTime nextRun = nextCronRun(claimed.recurringCron());
            jobs.completeAttempt(claimed.jobId(), claimed.workerName(), JobStatus.QUEUED, nextRun);
            jobs.resetAttemptsAfterRequeue(claimed.jobId());
            logEvent(claimed.jobId(), claimed.executionId(), JobLogLevel.ERROR,
                    "Attempt " + claimed.attemptNumber() + " failed (" + outcome.errorCode()
                            + "); recurring job requeued for " + nextRun);
            return;
        }
        boolean canRetry = claimed.attemptNumber() < claimed.maxAttempts();
        if (canRetry) {
            OffsetDateTime retryAt = BackoffPolicy.nextRetryTime(claimed.attemptNumber());
            jobs.completeAttempt(claimed.jobId(), claimed.workerName(), JobStatus.RETRY_WAIT, retryAt);
            logEvent(claimed.jobId(), claimed.executionId(), JobLogLevel.ERROR,
                    "Attempt " + claimed.attemptNumber() + " failed (" + outcome.errorCode()
                            + "); retry after backoff at " + retryAt);
        } else {
            jobs.completeAttempt(claimed.jobId(), claimed.workerName(), JobStatus.DEAD_LETTER,
                    OffsetDateTime.now());
            logEvent(claimed.jobId(), claimed.executionId(), JobLogLevel.ERROR,
                    "Attempt " + claimed.attemptNumber() + " failed permanently (" + outcome.errorCode()
                            + "); job moved to DEAD_LETTER");
        }
    }

    @Transactional
    public void finalizeCancelled(ClaimedJob claimed, ExecOutcome outcome) {
        closeExecution(claimed, ExecutionStatus.FAILED, outcome.durationMs(), null, "CANCELLED", null);
        jobs.completeAttempt(claimed.jobId(), claimed.workerName(), JobStatus.CANCELLED, OffsetDateTime.now());
        logEvent(claimed.jobId(), claimed.executionId(), JobLogLevel.WARN,
                "Attempt " + claimed.attemptNumber() + " cancelled mid-run");
    }

    private void closeExecution(ClaimedJob claimed, ExecutionStatus status, long durationMs,
                                com.fasterxml.jackson.databind.JsonNode result,
                                String errorCode, String errorMessage) {
        executions.findById(claimed.executionId()).ifPresent(exec -> {
            exec.setWorkerId(claimed.workerName());
            exec.markFinished(status, OffsetDateTime.now(), durationMs,
                    result == null ? null : result.toString(), errorCode, truncate(errorMessage));
            executions.save(exec);
        });
    }

    private void logEvent(UUID jobId, UUID executionId, JobLogLevel level, String message) {
        try {
            logs.save(new com.taskflow.execution.JobLog(jobs.getReferenceById(jobId), executionId, level, message));
        } catch (Exception e) {
            log.warn("Failed to persist job log: {}", e.getMessage());
        }
    }

    private OffsetDateTime nextCronRun(String cron) {
        return org.springframework.scheduling.support.CronExpression.parse(cron).next(OffsetDateTime.now());
    }

    static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    static String safeMessage(String message) {
        if (message == null) {
            return "Unknown handler error";
        }
        String cleaned = message.replaceAll("(?i)(api[_-]?key|authorization|bearer)\\s*[:=]\\s*(\\S+\\s*)+", "$1=***");
        return truncate(cleaned);
    }

    static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }

    /** Snapshot of the locked job row - avoids lazy loads after the claim tx commits. */
    private record JobEntitySnapshot(short attemptNumber, com.taskflow.job.JobType jobType, String payloadJson,
                                     int timeoutSeconds, short maxAttempts, String recurringCron,
                                     UUID executionId) {

        static JobEntitySnapshot of(com.taskflow.job.Job job) {
            // attemptNumber equals jobs.attempts because claimJob already incremented it
            return new JobEntitySnapshot(job.getAttempts(), job.getJobType(), job.getPayload(),
                    job.getTimeoutSeconds(), job.getMaxAttempts(), job.getRecurringCron(), UUID.randomUUID());
        }

        ClaimedJob toClaimedJob(String workerName, UUID jobId) {
            return new ClaimedJob(jobId, workerName, jobType, payloadJson, timeoutSeconds,
                    attemptNumber, maxAttempts, recurringCron, executionId);
        }
    }
}
