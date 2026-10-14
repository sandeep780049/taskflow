package com.taskflow.engine;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "taskflow")
public record EngineProperties(Worker worker, Scheduler scheduler, Recovery recovery) {

    public record Worker(boolean enabled, String name, int concurrency, int leaseSeconds,
                         int heartbeatSeconds, long pollIntervalMs) {
    }

    public record Scheduler(long tickMs) {
    }

    public record Recovery(long tickMs, int workerStaleSeconds) {
    }
}
