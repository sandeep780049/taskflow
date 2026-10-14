package com.taskflow.job;

import java.time.Duration;
import java.time.OffsetDateTime;

/** Exponential backoff: 15s, 30s, 60s ... capped at 1 hour. */
public final class BackoffPolicy {

    private static final long BASE_SECONDS = 15;
    private static final long CAP_SECONDS = 3600;

    private BackoffPolicy() {
    }

    public static OffsetDateTime nextRetryTime(int attemptsSoFar) {
        long shift = Math.min(attemptsSoFar - 1, 10);
        long seconds = Math.min(BASE_SECONDS * (1L << shift), CAP_SECONDS);
        return OffsetDateTime.now().plus(Duration.ofSeconds(seconds));
    }
}
