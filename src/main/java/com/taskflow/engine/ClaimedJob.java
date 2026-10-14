package com.taskflow.engine;

import com.taskflow.job.JobType;

import java.util.UUID;

/** Immutable snapshot taken while the job row was locked, then used off-transaction. */
public record ClaimedJob(
        UUID jobId,
        String workerName,
        JobType jobType,
        String payloadJson,
        int timeoutSeconds,
        short attemptNumber,
        short maxAttempts,
        String recurringCron,
        UUID executionId) {
}
