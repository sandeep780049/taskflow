package com.taskflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.taskflow.common.BadRequestException;
import com.taskflow.engine.JobHandler;
import org.springframework.stereotype.Component;

/**
 * Sleeps for a bounded duration to demonstrate real (long-running) execution,
 * lease renewal, and graceful mid-run cancellation.
 */
@Component("waitHandler")
public class WaitHandler implements JobHandler {

    private static final long MAX_MS = 120_000;

    @Override
    public String type() {
        return "WAIT";
    }

    @Override
    public JsonNode validatePayload(JsonNode payload) {
        if (payload == null || payload.isNull()) {
            return payload; // default duration below
        }
        JsonNode duration = payload.get("duration_ms");
        if (duration != null && !duration.isNull()) {
            if (!duration.canConvertToLong() || duration.asLong() < 0 || duration.asLong() > MAX_MS) {
                throw new BadRequestException("payload.duration_ms must be between 0 and " + MAX_MS);
            }
        }
        return payload;
    }

    @Override
    public JsonNode execute(JsonNode payload, ExecutionContext ctx) throws Exception {
        long durationMs = 5000;
        if (payload != null && payload.has("duration_ms")) {
            durationMs = payload.get("duration_ms").asLong();
        }
        long deadline = System.currentTimeMillis() + durationMs;
        while (System.currentTimeMillis() < deadline) {
            if (ctx.cancelled().getAsBoolean()) {
                ObjectNode out = JsonNodeFactory.instance.objectNode();
                out.put("slept_ms", System.currentTimeMillis() - (deadline - durationMs) + durationMs);
                out.put("status", "cancelled");
                return out;
            }
            Thread.sleep(100);
        }
        ObjectNode out = JsonNodeFactory.instance.objectNode();
        out.put("slept_ms", durationMs);
        out.put("status", "completed");
        return out;
    }
}
