package com.taskflow.job.dto;

import com.taskflow.execution.JobLog;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LogResponse(UUID id, UUID executionId, String level, String message, OffsetDateTime timestamp) {
    public static LogResponse from(JobLog log) {
        return new LogResponse(log.getId(), log.getExecutionId(), log.getLevel().name(), log.getMessage(), log.getTimestamp());
    }
}
