package com.taskflow.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController implements HealthIndicator {

    /**
     * Simple liveness endpoint used by Render's health check.
     * Returns UP as soon as the JVM is running — does not require the database.
     * The full /actuator/health endpoint still checks the database for operational monitoring.
     */
    @GetMapping("/health")
    public Health liveness() {
        return Health.up().build();
    }

    @Override
    public Health health() {
        return Health.up().build();
    }
}
