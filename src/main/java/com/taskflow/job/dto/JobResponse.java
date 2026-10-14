package com.taskflow.job.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.job.Job;
import com.taskflow.job.JobStatus;
import com.taskflow.job.JobType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobResponse(
        UUID id, String name, String description, JobType jobType, JsonNode payload, JobStatus status,
        int priority, OffsetDateTime scheduledAt, String recurringCron, String timezone,
        int maxAttempts, int attempts, int timeoutSeconds, OffsetDateTime nextAttemptAt,
        String idempotencyKey, String lockedBy, OffsetDateTime lockedUntil,
        boolean cancelRequested, OffsetDateTime createdAt, OffsetDateTime updatedAt, long version) {

    public static JobResponse from(Job job, ObjectMapper mapper) {
        return new JobResponse(
                job.getId(), job.getName(), job.getDescription(), job.getJobType(),
                parseQuiet(job.getPayload(), mapper), job.getStatus(),
                job.getPriority(), job.getScheduledAt(), job.getRecurringCron(), job.getTimezone(),
                job.getMaxAttempts(), job.getAttempts(), job.getTimeoutSeconds(), job.getNextAttemptAt(),
                job.getIdempotencyKey(), job.getLockedBy(), job.getLockedUntil(),
                job.isCancelRequested(), job.getCreatedAt(), job.getUpdatedAt(), job.getVersionStamp());
    }

    static JsonNode parseQuiet(String json, ObjectMapper mapper) {
        try {
            return json == null ? null : mapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
