package com.taskflow.job;

import com.taskflow.execution.JobExecution;
import com.taskflow.user.UserAccount;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private JobType jobType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.SCHEDULED;

    @Column(nullable = false)
    private short priority = 5;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "recurring_cron")
    private String recurringCron;

    @Column
    private String timezone;

    @Column(name = "max_attempts", nullable = false)
    private short maxAttempts = 3;

    @Column(nullable = false)
    private short attempts = 0;

    @Column(name = "timeout_seconds", nullable = false)
    private int timeoutSeconds = 60;

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt = OffsetDateTime.now();

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "locked_by")
    private String lockedBy;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "cancel_requested", nullable = false)
    private boolean cancelRequested = false;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version = 0L;

    @OneToMany(mappedBy = "job", fetch = FetchType.LAZY)
    @OrderBy("attemptNumber DESC")
    private transient List<JobExecution> executions = new ArrayList<>();

    public Job() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UserAccount getOwner() { return owner; }
    public void setOwner(UserAccount owner) { this.owner = owner; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public JobType getJobType() { return jobType; }
    public void setJobType(JobType jobType) { this.jobType = jobType; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public short getPriority() { return priority; }
    public void setPriority(short priority) { this.priority = priority; }
    public OffsetDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(OffsetDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public String getRecurringCron() { return recurringCron; }
    public void setRecurringCron(String recurringCron) { this.recurringCron = recurringCron; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public short getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(short maxAttempts) { this.maxAttempts = maxAttempts; }
    public short getAttempts() { return attempts; }
    public void setAttempts(short attempts) { this.attempts = attempts; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public OffsetDateTime getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(OffsetDateTime nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }
    public OffsetDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(OffsetDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public boolean isCancelRequested() { return cancelRequested; }
    public void setCancelRequested(boolean cancelRequested) { this.cancelRequested = cancelRequested; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public long getVersionStamp() { return version; }
    public List<JobExecution> getExecutions() { return executions; }
    public void setExecutions(List<JobExecution> executions) { this.executions = executions; }
}
