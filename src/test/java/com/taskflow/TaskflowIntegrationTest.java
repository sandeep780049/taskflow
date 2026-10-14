package com.taskflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-stack integration test against a real PostgreSQL (Testcontainers).
 * Exercises API -> scheduler -> worker -> retry -> dead-letter in-process.
 * Automatically skipped on machines without Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class TaskflowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    TestRestTemplate http;

    private String token;

    @Test
    void echoJobRunsToSuccess() {
        loginIfEmpty();
        var jobId = createJob("ECHO", Map.of("message", "hi"), null);
        awaitJobStatus(jobId, "SUCCEEDED", Duration.ofSeconds(60));
    }

    @Test
    void failingJobRetriesThenSucceeds() {
        loginIfEmpty();
        // fails the 1st attempt, succeeds on attempt 2
        var jobId = createJob("DEMO_FAILURE", Map.of("fail_times", 1), null, 2);
        awaitJobStatus(jobId, "SUCCEEDED", Duration.ofSeconds(90));
        assertAttempts(jobId, 2);
    }

    @Test
    void exhaustedRetriesLandInDeadLetter() {
        loginIfEmpty();
        var jobId = createJob("DEMO_FAILURE", Map.of("fail_times", 3), null, 2);
        awaitJobStatus(jobId, "DEAD_LETTER", Duration.ofSeconds(120));
        assertAttempts(jobId, 2);
    }

    // ---- helpers ----

    private HttpEntity<Map<String, Object>> json(Map<String, ?> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>((Map<String, Object>) (Map<?, ?>) body, headers);
    }

    private void loginIfEmpty() {
        if (token != null) {
            return;
        }
        String email = "it-" + UUID.randomUUID() + "@example.com";
        http.postForEntity(URI.create("/api/auth/register"),
                json(Map.of("email", email, "password", "integration-pass-1")), Map.class);
        var response = http.postForEntity(URI.create("/api/auth/login"),
                json(Map.of("email", email, "password", "integration-pass-1")), Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        token = (String) response.getBody().get("token");
        assertThat(token).isNotBlank();
    }

    private UUID createJob(String type, Map<String, Object> payload, String cron) {
        return createJob(type, payload, cron, 3);
    }

    private UUID createJob(String type, Map<String, Object> payload, String cron, int maxAttempts) {
        Map<String, Object> body;
        if (cron != null) {
            body = Map.of("name", "it-" + type, "jobType", type, "payload", payload,
                    "recurringCron", cron, "maxAttempts", maxAttempts);
        } else {
            body = Map.of("name", "it-" + type, "jobType", type, "payload", payload, "maxAttempts", maxAttempts);
        }
        var response = http.exchange(URI.create("/api/jobs"), org.springframework.http.HttpMethod.POST,
                entity(body), Map.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Job creation failed: " + response.getBody());
        }
        return UUID.fromString((String) response.getBody().get("id"));
    }

    private HttpEntity<Object> entity(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jobDetail(UUID jobId) {
        var response = http.exchange(URI.create("/api/jobs/" + jobId), org.springframework.http.HttpMethod.GET,
                entity(Map.of()), Map.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Job detail failed: " + response.getBody());
        }
        return (Map<String, Object>) response.getBody().get("job");
    }

    private void awaitJobStatus(UUID jobId, String expected, Duration timeout) {
        BooleanSupplier done = () -> expected.equals(jobDetail(jobId).get("status"));
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!done.getAsBoolean()) {
            assertThat(System.nanoTime()).describedAs("timeout waiting for job %s -> %s", jobId, expected)
                    .isLessThan(deadline);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    private void assertAttempts(UUID jobId, int expected) {
        Map<String, Object> job = jobDetail(jobId);
        assertThat(((Number) job.get("attempts")).intValue()).isEqualTo(expected);
    }
}
