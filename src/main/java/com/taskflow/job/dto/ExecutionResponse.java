package com.taskflow.job.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.execution.ExecutionStatus;
import com.taskflow.execution.JobExecution;
import com.taskflow.job.dto.JobResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ExecutionResponse(
        UUID id, short attemptNumber, String workerId, ExecutionStatus status,
        OffsetDateTime startedAt, OffsetDateTime finishedAt, Long durationMs,
        JsonNode result, String errorCode, String errorMessage) {

    public static ExecutionResponse from(JobExecution exec, ObjectMapper mapper) {
        return new ExecutionResponse(
                exec.getId(), exec.getAttemptNumber(), exec.getWorkerId(), exec.getStatus(),
                exec.getStartedAt(), exec.getFinishedAt(), exec.getDurationMs(),
                JobResponse.parseQuiet(exec.getResult(), mapper), exec.getErrorCode(), exec.getErrorMessage());
    }
}
