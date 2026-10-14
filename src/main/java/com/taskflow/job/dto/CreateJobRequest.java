package com.taskflow.job.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;

public record CreateJobRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 2000) String description,
        @NotNull String jobType,
        JsonNode payload,
        @Min(1) @Max(9) Integer priority,
        OffsetDateTime scheduledAt,
        String recurringCron,
        String timezone,
        @Min(1) @Max(10) Integer maxAttempts,
        @Min(1) @Max(600) Integer timeoutSeconds,
        @Size(max = 120) String idempotencyKey) {
}
