package com.taskflow.metrics;

import com.taskflow.auth.JwtAuthFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics")
public class MetricsController {

    private final MetricsService metrics;

    public MetricsController(MetricsService metrics) {
        this.metrics = metrics;
    }

    @GetMapping
    @Operation(summary = "Dashboard metrics: jobs by status, 24h success rate, hourly chart buckets, worker health")
    public MetricsService.MetricsSummary metrics() {
        return metrics.summary(JwtAuthFilter.currentUser());
    }
}
