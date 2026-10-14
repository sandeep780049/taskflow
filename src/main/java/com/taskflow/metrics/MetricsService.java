package com.taskflow.metrics;

import com.taskflow.auth.JwtAuthFilter;
import com.taskflow.execution.JobExecutionRepository;
import com.taskflow.job.JobRepository;
import com.taskflow.worker.WorkerRepository;
import com.taskflow.worker.WorkerStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MetricsService {

    private final JobRepository jobs;
    private final JobExecutionRepository executions;
    private final WorkerRepository workers;

    public MetricsService(JobRepository jobs, JobExecutionRepository executions, WorkerRepository workers) {
        this.jobs = jobs;
        this.executions = executions;
        this.workers = workers;
    }

    record HourlyStats(String hour, long total, long succeeded, double avgDurationMs) {
    }

    public record MetricsSummary(Map<String, Long> jobsByStatus, double successRate24h,
                                 double avgDurationMs24h, List<HourlyStats> hourly,
                                 long activeWorkers, long staleWorkers) {
    }

    @Transactional(readOnly = true)
    public MetricsSummary summary(JwtAuthFilter.AuthenticatedUser user) {
        UUID ownerId = user.isAdmin() ? null : user.id();
        Map<String, Long> byStatus = new HashMap<>();
        List<Object[]> rows = ownerId == null ? jobs.countAllByStatusRaw() : jobs.countByStatusRaw(ownerId);
        for (Object[] row : rows) {
            byStatus.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        List<Object[]> hourlyRows = executions.hourlyStats24h(ownerId);
        List<HourlyStats> hourly = hourlyRows.stream()
                .map(r -> new HourlyStats(String.valueOf(r[0]), ((Number) r[1]).longValue(),
                        ((Number) r[2]).longValue(), ((Number) r[3]).doubleValue()))
                .toList();

        long total24h = hourly.stream().mapToLong(HourlyStats::total).sum();
        long succeeded24h = hourly.stream().mapToLong(HourlyStats::succeeded).sum();
        double successRate = total24h == 0 ? 0.0 : 100.0 * succeeded24h / total24h;
        double avgDuration = hourly.stream().mapToDouble(HourlyStats::avgDurationMs).average().orElse(0.0);

        long activeWorkers = workers.countByStatus(WorkerStatus.ACTIVE);
        long staleWorkers = workers.countByStatus(WorkerStatus.STALE);

        return new MetricsSummary(byStatus, successRate, avgDuration, hourly, activeWorkers, staleWorkers);
    }
}
