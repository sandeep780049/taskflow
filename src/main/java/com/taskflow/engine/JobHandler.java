package com.taskflow.engine;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * SPI for safe, built-in job types. Handlers must be pure functions of
 * validated inputs (plus documented side effects) and must be idempotent:
 * execution is at-least-once, never exactly-once.
 */
public interface JobHandler {

    String type();

    /** Validate and normalize the job payload; throw BadRequestException on bad input. */
    JsonNode validatePayload(JsonNode payload);

    /** Execute the job and return the JSON result to store on the execution record. */
    JsonNode execute(JsonNode payload, ExecutionContext ctx) throws Exception;

    /** Per-attempt context handed to handlers. */
    record ExecutionContext(UUID jobId, short attemptNumber, BooleanSupplier cancelled) {
    }
}
