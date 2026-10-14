package com.taskflow.engine;

import com.taskflow.common.ApiException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Aggregates every registered JobHandler and validates job types/payloads. */
@Component
public class HandlersRegistry {

    private final Map<String, JobHandler> byType = new HashMap<>();

    public HandlersRegistry(List<JobHandler> handlers) {
        for (JobHandler handler : handlers) {
            byType.put(handler.type().toUpperCase(), handler);
        }
    }

    public JobHandler require(String type) {
        JobHandler handler = byType.get(type == null ? "" : type.toUpperCase());
        if (handler == null) {
            throw ApiException.badRequest("Unknown job type: " + type + ". Supported: " + byType.keySet());
        }
        return handler;
    }

    public void validatePayload(String type, com.fasterxml.jackson.databind.JsonNode payload) {
        require(type).validatePayload(payload);
    }
}
