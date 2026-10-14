package com.taskflow.execution;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobLogRepository extends JpaRepository<JobLog, UUID> {

    List<JobLog> findByJobIdOrderByTimestampAsc(UUID jobId);
}
