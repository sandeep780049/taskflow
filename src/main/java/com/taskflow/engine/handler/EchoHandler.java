package com.taskflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.taskflow.common.BadRequestException;
import com.taskflow.engine.JobHandler;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Returns the supplied payload; the canonical happy-path job type. */
@Component("echoHandler")
public class EchoHandler implements JobHandler {

    @Override
    public String type() {
        return "ECHO";
    }

    @Override
    public JsonNode validatePayload(JsonNode payload) {
        // ECHO accepts any payload (including null, including arbitrarily nested JSON)
        return payload;
    }

    @Override
    public JsonNode execute(JsonNode payload, com.taskflow.engine.JobHandler.ExecutionContext ctx) {
        return payload;
    }
}
