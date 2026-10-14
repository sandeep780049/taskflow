package com.taskflow.engine.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskflow.common.BadRequestException;
import com.taskflow.engine.JobHandler;
import com.taskflow.engine.SsrfGuard;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Optional outbound HTTP GET with strict safety rules: public hosts only,
 * no redirects, hard timeouts, and a small response size cap.
 */
@Component("httpRequestHandler")
public class HttpRequestHandler implements JobHandler {

    private static final long MAX_RESPONSE_BYTES = 64 * 1024;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public String type() {
        return "HTTP_REQUEST";
    }

    @Override
    public JsonNode validatePayload(JsonNode payload) {
        if (payload == null || payload.isNull() || payload.get("url") == null
                || payload.get("url").asText().isBlank()) {
            throw new BadRequestException("HTTP_REQUEST requires payload.url (http(s) to a public host)");
        }
        SsrfGuard.validate(payload.get("url").asText());
        return payload;
    }

    @Override
    public JsonNode execute(JsonNode payload, ExecutionContext ctx) throws Exception {
        URI uri = SsrfGuard.validate(payload.get("url").asText());
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "TaskFlow-http-job/1.0")
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        ObjectNode out = JsonNodeFactory.instance.objectNode();
        out.put("status_code", response.statusCode());
        out.put("content_type", response.headers().firstValue("content-type").orElse(null));
        byte[] body = response.body();
        out.put("body_truncated", body != null && body.length > MAX_RESPONSE_BYTES);
        int len = body == null ? 0 : Math.min(body.length, (int) MAX_RESPONSE_BYTES);
        out.put("body_preview", new String(body, 0, len, java.nio.charset.StandardCharsets.UTF_8));
        out.put("elapsed", "recorded separately as duration_ms");
        return out;
    }
}
