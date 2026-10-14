package com.taskflow.engine;

import com.taskflow.job.JobRepository;
import com.taskflow.worker.WorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Embedded worker: claims due jobs from PostgreSQL and executes them with
 * bounded concurrency. Multiple instances of this app are multiple workers -
 * correctness comes from FOR UPDATE SKIP LOCKED + leases, not from this class.
 */
@Component
public class JobWorker {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);

    private final JobRepository jobs;
    private final JobExecutor executor;
    private final WorkerService workers;
    private final EngineProperties props;

    private final String workerName;
    private final ExecutorService claimPool;
    private final ExecutorService execPool;
    private final Semaphore permits;

    public JobWorker(JobRepository jobs, JobExecutor executor, WorkerService workers, EngineProperties props) {
        this.jobs = jobs;
        this.executor = executor;
        this.workers = workers;
        this.props = props;
        this.workerName = props.worker().name() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        this.claimPool = Executors.newSingleThreadExecutor(r -> new Thread(r, "tf-claim-loop"));
        this.execPool = Executors.newFixedThreadPool(Math.max(1, props.worker().concurrency()),
                r -> new Thread(r, "tf-worker-" + r.hashCode()));
        this.permits = new Semaphore(props.worker().concurrency());
    }

    public String getWorkerName() {
        return workerName;
    }

    /** One polling iteration; returns true if a job was claimed and dispatched. */
    boolean tick() {
        if (!permits.tryAcquire()) {
            return false;
        }
        try {
            var claimed = executor.claimOne(workerName);
            if (claimed.isEmpty()) {
                permits.release();
                return false;
            }
            var job = claimed.get();
            execPool.submit(() -> {
                ExecOutcome outcome;
                try {
                    outcome = executor.run(job);
                } catch (Exception e) {
                    log.error("Unexpected engine failure for job {}", job.jobId(), e);
                    outcome = ExecOutcome.failure("ENGINE_ERROR", JobExecutor.safeMessage(e.getMessage()), 0L);
                }
                try {
                    switch (outcome.outcome()) {
                        case SUCCESS -> executor.finalizeSuccess(job, outcome);
                        case CANCELLED -> executor.finalizeCancelled(job, outcome);
                        case FAILURE -> executor.finalizeError(job, outcome);
                    }
                } catch (Exception e) {
                    log.error("Finalization failed for job {} (outcome {}). The lease reaper will recover it.",
                            job.jobId(), outcome.outcome(), e);
                } finally {
                    permits.release();
                }
            });
            return true;
        } catch (Exception e) {
            permits.release();
            log.error("Claim loop error: {}", e.getMessage(), e);
            return false;
        }
    }

    void register() {
        try {
            workers.register(workerName, null);
        } catch (Exception e) {
            log.warn("Worker registration failed: {}", e.getMessage());
        }
    }

    ExecutorService claimLoop() {
        return claimPool;
    }

    long pollIntervalMs() {
        return props.worker().pollIntervalMs();
    }

    long heartbeatSeconds() {
        return props.worker().heartbeatSeconds();
    }
}
