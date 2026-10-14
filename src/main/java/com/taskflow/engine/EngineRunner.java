package com.taskflow.engine;

import com.taskflow.worker.WorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Owns the engine's background loops, started after the app is ready:
 *  - the worker claim/execute loop and its heartbeat (if worker.enabled),
 *  - the scheduler + lease-recovery recovery tick.
 */
@Component
public class EngineRunner implements org.springframework.context.SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(EngineRunner.class);

    private final EngineProperties props;
    private final LeaseReaper reaper;
    private final JobWorker worker;
    private final WorkerService workers;

    private volatile ScheduledExecutorService executor;
    private volatile boolean running = false;

    public EngineRunner(EngineProperties props, LeaseReaper reaper, JobWorker worker, WorkerService workers) {
        this.props = props;
        this.reaper = reaper;
        this.worker = worker;
        this.workers = workers;
    }

    @Override
    public void start() {
        running = true;
        executor = Executors.newScheduledThreadPool(3, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });

        // recovery: promote due jobs, recover expired leases, mark stale workers
        long recoveryMs = Math.max(props.recovery().tickMs(), 1000);
        executor.scheduleWithFixedDelay(this::recoveryTick, recoveryMs, recoveryMs, TimeUnit.MILLISECONDS);

        if (props.worker().enabled()) {
            worker.register();
            executor.scheduleWithFixedDelay(() -> {
                try {
                    workers.heartbeat(worker.getWorkerName());
                } catch (Exception e) {
                    log.warn("heartbeat failed: {}", e.getMessage());
                }
            }, props.worker().heartbeatSeconds(), props.worker().heartbeatSeconds(), TimeUnit.SECONDS);

            executor.scheduleWithFixedDelay(() -> {
                if (!running) {
                    return;
                }
                try {
                    int claimed = 0;
                    // drain all free execution slots each tick
                    while (worker.tick() && claimed++ < props.worker().concurrency()) {
                        Thread.yield();
                    }
                } catch (Exception e) {
                    log.error("claim loop crashed: {}", e.getMessage(), e);
                }
            }, 500, Math.max(props.worker().pollIntervalMs(), 250), TimeUnit.MILLISECONDS);
        }
        log.info("TaskFlow engine started: worker enabled={}, name={}, concurrency={}",
                props.worker().enabled(), worker.getWorkerName(), props.worker().concurrency());
    }

    private void recoveryTick() {
        if (!running) {
            return;
        }
        try {
            reaper.tick();
        } catch (Exception e) {
            log.error("recovery tick failed: {}", e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        running = false;
        if (executor != null) {
            executor.shutdownNow();
        }
        log.info("TaskFlow engine stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return org.springframework.context.SmartLifecycle.DEFAULT_PHASE;
    }
}
