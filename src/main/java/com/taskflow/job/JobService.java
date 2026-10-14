package com.taskflow.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.auth.JwtAuthFilter;
import com.taskflow.common.ApiException;
import com.taskflow.engine.HandlersRegistry;
import com.taskflow.execution.JobLogLevel;
import com.taskflow.execution.JobLogRepository;
import com.taskflow.job.dto.CreateJobRequest;
import com.taskflow.job.dto.JobDetailResponse;
import com.taskflow.job.dto.JobPageResponse;
import com.taskflow.job.dto.JobResponse;
import com.taskflow.execution.JobExecutionRepository;
import com.taskflow.user.UserAccount;
import com.taskflow.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class JobService {

    private static final int MAX_PAGE_SIZE = 100;

    private final JobRepository jobs;
    private final UserRepository users;
    private final JobExecutionRepository executions;
    private final JobLogRepository logs;
    private final HandlersRegistry handlers;
    private final ObjectMapper mapper;

    public JobService(JobRepository jobs, UserRepository users, JobExecutionRepository executions,
                      JobLogRepository logs, HandlersRegistry handlers, ObjectMapper mapper) {
        this.jobs = jobs;
        this.users = users;
        this.executions = executions;
        this.logs = logs;
        this.handlers = handlers;
        this.mapper = mapper;
    }

    @Transactional
    public Job create(UUID ownerId, CreateJobRequest req) {
        // Validate job type + payload before anything is written
        handlers.validatePayload(req.jobType(), req.payload());

        if (req.scheduledAt() != null && req.scheduledAt().isBefore(OffsetDateTime.now().minusSeconds(5))) {
            throw ApiException.badRequest("scheduledAt must be in the future");
        }
        if (req.recurringCron() != null) {
            if (!CronExpression.isValidExpression(req.recurringCron())) {
                throw ApiException.badRequest(
                        "recurringCron is not a valid cron expression (6 fields, e.g. \"0 0 2 * * *\")");
            }
            if (req.timezone() != null && !ZoneId.getAvailableZoneIds().contains(req.timezone())) {
                throw ApiException.badRequest("Unknown timezone: " + req.timezone());
            }
        }

        JobStatus initial = req.scheduledAt() != null ? JobStatus.SCHEDULED : JobStatus.QUEUED;

        Job job = new Job();
        job.setId(UUID.randomUUID());
        UserAccount ownerRef = users.getReferenceById(ownerId);
        job.setOwner(ownerRef);
        job.setName(req.name().trim());
        job.setDescription(req.description());
        job.setJobType(com.taskflow.job.JobType.valueOf(req.jobType().trim().toUpperCase()));
        job.setPayload(req.payload() == null ? null : req.payload().toString());
        job.setStatus(initial);
        job.setPriority(req.priority() == null ? (short) 5 : req.priority().shortValue());
        job.setScheduledAt(req.scheduledAt());
        job.setRecurringCron(req.recurringCron());
        job.setTimezone(req.timezone());
        job.setMaxAttempts(req.maxAttempts() == null ? (short) 3 : req.maxAttempts().shortValue());
        job.setTimeoutSeconds(req.timeoutSeconds() == null ? 60 : req.timeoutSeconds());
        job.setIdempotencyKey(req.idempotencyKey());
        job.setNextAttemptAt(initial == JobStatus.SCHEDULED ? req.scheduledAt() : OffsetDateTime.now());
        Job saved = jobs.save(job);
        log(saved, null, JobLogLevel.INFO, "Job created with status " + initial);
        return saved;
    }

    @Transactional(readOnly = true)
    public JobPageResponse list(JwtAuthFilter.AuthenticatedUser user, JobStatus status, int page, int size) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        UUID ownerId = user.isAdmin() ? null : user.id();
        var sort = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Job> result;
        if (ownerId == null) {
            result = status == null ? jobs.findAll(sort) : jobs.findByStatus(status, sort);
        } else {
            result = status == null ? jobs.findByOwnerId(ownerId, sort) : jobs.findByOwnerIdAndStatus(ownerId, status, sort);
        }
        var items = result.getContent().stream().map(j -> JobResponse.from(j, mapper)).toList();
        return new JobPageResponse(result.getNumber() + 1, result.getTotalPages(), result.getTotalElements(), items);
    }

    @Transactional(readOnly = true)
    public JobDetailResponse detail(UUID ownerId, boolean isAdmin, UUID id) {
        assertAccess(ownerId, isAdmin, id);
        Job job = jobs.findWithOwnerById(id).orElseThrow(() -> ApiException.notFound("Job not found"));
        var execList = executions.findByJobIdOrderByAttemptNumberDesc(id).stream()
                .map(e -> com.taskflow.job.dto.ExecutionResponse.from(e, mapper)).toList();
        var logList = logs.findByJobIdOrderByTimestampAsc(id).stream()
                .map(com.taskflow.job.dto.LogResponse::from).toList();
        return new JobDetailResponse(JobResponse.from(job, mapper), execList, logList);
    }

    /** Cancel a job: waiting jobs cancel immediately, running jobs gracefully. */
    @Transactional
    public boolean cancel(UUID ownerId, boolean isAdmin, UUID id) {
        assertAccess(ownerId, isAdmin, id);
        Job job = jobs.findWithOwnerById(id).orElseThrow(() -> ApiException.notFound("Job not found"));
        switch (job.getStatus()) {
            case RUNNING -> {
                int rows = jobs.requestCancel(id);
                if (rows > 0) {
                    log(job, null, JobLogLevel.WARN, "Cancellation requested while running");
                    return true;
                }
                throw ApiException.conflict("Job state changed concurrently; retry cancel");
            }
            case SUCCEEDED, CANCELLED, DEAD_LETTER -> {
                return false; // already terminal, idempotent cancel
            }
            default -> {
                int rows = jobs.cancelWaiting(id);
                if (rows == 0) {
                    throw ApiException.conflict("Job state changed concurrently; retry cancel");
                }
                log(job, null, JobLogLevel.WARN, "Job cancelled by owner");
                return true;
            }
        }
    }

    @Transactional
    public void delete(UUID ownerId, boolean isAdmin, UUID id) {
        assertAccess(ownerId, isAdmin, id);
        Job job = jobs.findWithOwnerById(id).orElseThrow(() -> ApiException.notFound("Job not found"));
        if (job.getStatus() == JobStatus.RUNNING) {
            throw ApiException.conflict("Cannot delete a running job; cancel it first");
        }
        if (isAdmin) {
            jobs.deleteById(id);
        } else {
            jobs.deleteByIdAndOwnerId(id, ownerId);
        }
    }

    public void assertAccess(UUID ownerId, boolean isAdmin, UUID jobId) {
        if (isAdmin) {
            return;
        }
        UUID actual = jobs.findOwnerId(jobId).orElse(null);
        if (actual == null || !actual.equals(ownerId)) {
            throw ApiException.notFound("Job not found"); // do not leak existence
        }
    }

    private void log(Job job, UUID executionId, JobLogLevel level, String message) {
        logs.save(new com.taskflow.execution.JobLog(job, executionId, level, message));
    }
}
