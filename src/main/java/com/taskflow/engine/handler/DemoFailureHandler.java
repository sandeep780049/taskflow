package com.taskflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskflow.common.BadRequestException;
import com.taskflow.engine.JobHandler;
import org.springframework.stereotype.Component;

/** Deliberately fails the first N attempts so retry/backoff/dead-letter can be exercised. */
@Component("demoFailureHandler")
public class DemoFailureHandler implements JobHandler {

    @Override
    public String type() {
        return "DEMO_FAILURE";
    }

    @Override
    public JsonNode validatePayload(JsonNode payload) {
        if (payload != null && !payload.isNull() && payload.has("fail_times")) {
            JsonNode times = payload.get("fail_times");
            if (!times.isInt() || times.asInt() < 1 || times.asInt() > 10) {
                throw new BadRequestException("payload.fail_times must be an integer between 1 and 10");
            }
        }
        return payload;
    }

    @Override
    public JsonNode execute(JsonNode payload, ExecutionContext ctx) {
        int failTimes = payload != null && payload.has("fail_times") ? payload.get("fail_times").asInt() : 1;
        if (ctx.attemptNumber() <= failTimes) {
            ObjectNode err = JsonNodeFactory.instance.objectNode();
            err.put("attempt", ctx.attemptNumber());
            err.put("planned_failures", failTimes);
            throw new DemoFailureException("DEMO_FAILURE: intentionally failing attempt " + ctx.attemptNumber());
        }
        ObjectNode out = JsonNodeFactory.instance.objectNode();
        out.put("message", "succeeded on attempt " + ctx.attemptNumber());
        return out;
    }

    public static class DemoFailureException extends RuntimeException {
        public DemoFailureException(String message) {
            super(message);
        }
    }
}
