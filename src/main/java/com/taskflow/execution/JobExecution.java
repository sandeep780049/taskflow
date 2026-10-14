package com.taskflow.execution;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_executions")
public class JobExecution {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private com.taskflow.job.Job job;

    @Column(name = "attempt_number", nullable = false)
    private short attemptNumber;

    @Column(name = "worker_id")
    private String workerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @Column(name = "started_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String result;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public JobExecution() {
    }

    public JobExecution(UUID id, com.taskflow.job.Job job, short attemptNumber) {
        this.id = id;
        this.job = job;
        this.attemptNumber = attemptNumber;
        this.status = ExecutionStatus.RUNNING;
        this.startedAt = OffsetDateTime.now();
    }

    public void markFinished(ExecutionStatus status, OffsetDateTime finishedAt, long durationMs,
                             String result, String errorCode, String errorMessage) {
        this.status = status;
        this.finishedAt = finishedAt;
        this.durationMs = durationMs;
        this.result = result;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public UUID getId() { return id; }
    public com.taskflow.job.Job getJob() { return job; }
    public short getAttemptNumber() { return attemptNumber; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public ExecutionStatus getStatus() { return status; }
    public void setStatus(ExecutionStatus status) { this.status = status; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public Long getDurationMs() { return durationMs; }
    public String getResult() { return result; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
}
