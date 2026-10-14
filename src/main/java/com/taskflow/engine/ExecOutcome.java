package com.taskflow.engine;

import com.fasterxml.jackson.databind.JsonNode;

/** Result of an off-transaction handler run; DB finalization happens separately. */
public record ExecOutcome(Outcome outcome, JsonNode result, String errorCode, String errorMessage,
                          long durationMs) {

    public enum Outcome { SUCCESS, CANCELLED, FAILURE }

    public static ExecOutcome success(JsonNode result, long durationMs) {
        return new ExecOutcome(Outcome.SUCCESS, result, null, null, durationMs);
    }

    public static ExecOutcome cancelled(long durationMs) {
        return new ExecOutcome(Outcome.CANCELLED, null, "CANCELLED", null, durationMs);
    }

    public static ExecOutcome failure(String errorCode, String errorMessage, long durationMs) {
        return new ExecOutcome(Outcome.FAILURE, null, errorCode, errorMessage, durationMs);
    }
}
