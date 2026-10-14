package com.taskflow.job.dto;

import java.util.List;

public record JobDetailResponse(JobResponse job, List<ExecutionResponse> executions, List<LogResponse> logs) {
}
