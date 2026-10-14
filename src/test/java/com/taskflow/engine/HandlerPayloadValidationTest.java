package com.taskflow.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.common.BadRequestException;
import com.taskflow.engine.handler.DemoFailureHandler;
import com.taskflow.engine.handler.EchoHandler;
import com.taskflow.engine.handler.HttpRequestHandler;
import com.taskflow.engine.handler.WaitHandler;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HandlerPayloadValidationTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final EchoHandler echo = new EchoHandler();
    private final WaitHandler wait = new WaitHandler();
    private final HttpRequestHandler http = new HttpRequestHandler();
    private final DemoFailureHandler demo = new DemoFailureHandler();

    private static JobHandler.ExecutionContext ctx(short attempt) {
        return new JobHandler.ExecutionContext(UUID.randomUUID(), attempt, () -> false);
    }

    @Test
    void echoAcceptsAnything() {
        assertThat(echo.validatePayload(null)).isNull();
        assertThat(echo.execute(mapper.valueToTree(java.util.Map.of("a", 1)), ctx((short) 1)).get("a").asInt()).isEqualTo(1);
    }

    @Test
    void waitRejectsUnboundedDurations() {
        assertThatThrownBy(() -> wait.validatePayload(payload("{\"duration_ms\": 999000}")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> wait.validatePayload(payload("{\"duration_ms\": -5}")))
                .isInstanceOf(BadRequestException.class);
        assertThat(wait.validatePayload(null)).isNull();
    }

    @Test
    void waitHonoursCancellationMidRun() throws Exception {
        var payload = wait.validatePayload(payload("{\"duration_ms\": 30000}"));
        var cancelledCtx = new JobHandler.ExecutionContext(UUID.randomUUID(), (short) 1, () -> true);
        var result = wait.execute(payload, cancelledCtx);
        assertThat(result.get("status").asText()).isEqualTo("cancelled");
    }

    @Test
    void httpRequiresUrl() {
        assertThatThrownBy(() -> http.validatePayload(null)).isInstanceOf(BadRequestException.class);
        // SSRF guard failures surface as ApiException.badRequest
        assertThatThrownBy(() -> http.validatePayload(payload("{\"url\": \"http://127.0.0.1/x\"}")))
                .isInstanceOf(com.taskflow.common.ApiException.class);
        var ok = http.validatePayload(payload("{\"url\": \"https://github.com/status\"}"));
        assertThat(ok.get("url").asText()).isEqualTo("https://github.com/status");
    }

    @Test
    void demoFailureFailsNAttemptsThenSucceeds() {
        demo.validatePayload(payload("{\"fail_times\": 2}"));
        assertThatThrownBy(() -> demo.execute(payload("{\"fail_times\": 2}"), ctx((short) 1)))
                .isInstanceOf(DemoFailureHandler.DemoFailureException.class);
        assertThatThrownBy(() -> demo.execute(payload("{\"fail_times\": 2}"), ctx((short) 2)))
                .isInstanceOf(DemoFailureHandler.DemoFailureException.class);
        assertThat(demo.execute(payload("{\"fail_times\": 2}"), ctx((short) 3)).get("message").asText())
                .contains("attempt 3");
    }

    @Test
    void demoFailureValidatesBounds() {
        assertThatThrownBy(() -> demo.validatePayload(payload("{\"fail_times\": 0}")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> demo.validatePayload(payload("{\"fail_times\": 99}")))
                .isInstanceOf(BadRequestException.class);
    }

    private JsonNode payload(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
