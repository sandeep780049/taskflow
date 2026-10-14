package com.taskflow.execution;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_logs")
public class JobLog {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private com.taskflow.job.Job job;

    @Column(name = "execution_id")
    private UUID executionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobLogLevel level = JobLogLevel.INFO;

    @Column(nullable = false)
    private String message;

    @Column(name = "timestamp", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime timestamp;

    public JobLog() {
    }

    public JobLog(com.taskflow.job.Job job, UUID executionId, JobLogLevel level, String message) {
        this.id = UUID.randomUUID();
        this.job = job;
        this.executionId = executionId;
        this.level = level;
        // Preserve the log content but enforce a sane bound in memory too
        this.message = message != null && message.length() > 4000
                ? message.substring(0, 4000)
                : message;
    }

    public UUID getId() { return id; }
    public com.taskflow.job.Job getJob() { return job; }
    public UUID getExecutionId() { return executionId; }
    public JobLogLevel getLevel() { return level; }
    public String getMessage() { return message; }
    public OffsetDateTime getTimestamp() { return timestamp; }
}
