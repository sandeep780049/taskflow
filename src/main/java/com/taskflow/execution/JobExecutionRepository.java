package com.taskflow.execution;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {

    List<JobExecution> findByJobIdOrderByAttemptNumberDesc(UUID jobId);

    Optional<JobExecution> findByJobIdAndAttemptNumber(UUID jobId, short attemptNumber);

    long deleteByJobId(UUID jobId);

    // Helpful aggregation endpoints for the dashboard
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT date_trunc('hour', started_at) AS bucket,
                   count(*) AS total,
                   count(*) FILTER (WHERE status = 'SUCCEEDED') AS succeeded,
                   coalesce(avg(duration_ms) FILTER (WHERE finished_at IS NOT NULL), 0) AS avg_ms
            FROM job_executions
            WHERE started_at >= now() - interval '24 hours' AND (:ownerId IS NULL OR job_id IN (
                SELECT id FROM jobs WHERE owner_id = :ownerId))
            GROUP BY bucket ORDER BY bucket
            """, nativeQuery = true)
    List<Object[]> hourlyStats24h(@org.springframework.data.repository.query.Param("ownerId") UUID ownerId);
}
