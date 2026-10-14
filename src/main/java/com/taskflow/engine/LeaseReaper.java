package com.taskflow.engine;

import com.taskflow.execution.ExecutionStatus;
import com.taskflow.execution.JobExecution;
import com.taskflow.execution.JobExecutionRepository;
import com.taskflow.execution.JobLogLevel;
import com.taskflow.execution.JobLogRepository;
import com.taskflow.job.JobRepository;
import com.taskflow.job.JobStatus;
import com.taskflow.worker.WorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Recovery service. Every recovery tick:
 *  - promotes due SCHEDULED and RETRY_WAIT jobs to QUEUED,
 *  - recovers RUNNING jobs whose worker lease expired (worker crash),
 *  - marks workers whose heartbeat is stale.
 */
@Component
public class LeaseReaper {

    private static final Logger log = LoggerFactory.getLogger(LeaseReaper.class);
    private static final int RECOVERY_BATCH = 50;

    private final JobRepository jobs;
    private final JobExecutionRepository executions;
    private final JobLogRepository logs;
    private final WorkerService workers;
    private final EngineProperties props;

    public LeaseReaper(JobRepository jobs, JobExecutionRepository executions, JobLogRepository logs,
                       WorkerService workers, EngineProperties props) {
        this.jobs = jobs;
        this.executions = executions;
        this.logs = logs;
        this.workers = workers;
        this.props = props;
    }

    @Transactional
    public void tick() {
        int scheduled = jobs.promoteDueScheduledJobs();
        int retry = jobs.promoteRetryWaitingJobs();
        int recovered = recoverExpiredLeases();
        int stale = workers.markStaleWorkers(props.recovery().workerStaleSeconds());
        if (scheduled + retry + recovered + stale > 0) {
            log.info("recovery tick: scheduled+{} retry+{} recovered+{} staleWorkers+{}",
                    scheduled, retry, recovered, stale);
        }
    }

    private int recoverExpiredLeases() {
        var ids = jobs.findExpiredLeaseIds(RECOVERY_BATCH);
        int recovered = 0;
        for (UUID jobId : ids) {
            var job = jobs.findById(jobId).orElse(null);
            if (job == null) {
                continue;
            }
            boolean canRetryAgain = job.getAttempts() < job.getMaxAttempts();
            if (canRetryAgain) {
                jobs.recoverExpiredLease(jobId, JobStatus.QUEUED, OffsetDateTime.now());
            } else {
                jobs.recoverExpiredLease(jobId, JobStatus.DEAD_LETTER, OffsetDateTime.now());
            }
            closeOrphanExecution(jobId, job.getAttempts());
            logs.save(new com.taskflow.execution.JobLog(jobs.getReferenceById(jobId), null, JobLogLevel.WARN,
                    "Lease expired; " + (canRetryAgain ? "requeued for another attempt" : "moved to DEAD_LETTER")));
            recovered++;
        }
        return recovered;
    }

    private void closeOrphanExecution(UUID jobId, short attempts) {
        executions.findByJobIdAndAttemptNumber(jobId, attempts).ifPresent(exec -> {
            if (exec.getStatus() == ExecutionStatus.RUNNING) {
                exec.setWorkerId(exec.getWorkerId());
                exec.markFinished(ExecutionStatus.FAILED, OffsetDateTime.now(), 0, null, "LEASE_EXPIRED",
                        "Worker lost its lease before reporting the result");
                executions.save(exec);
            }
        });
    }
}
