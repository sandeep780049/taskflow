package com.taskflow.job;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BackoffPolicyTest {

    @Test
    void growsExponentially() {
        var t1 = BackoffPolicy.nextRetryTime(1);
        var t2 = BackoffPolicy.nextRetryTime(2);
        var t3 = BackoffPolicy.nextRetryTime(3);
        // 15s, 30s, 60s backoff slots (allow a small scheduling skew)
        assertThat(Duration.between(OffsetDateTime.now(), t1)).isGreaterThan(Duration.ofSeconds(14));
        assertThat(Duration.between(OffsetDateTime.now(), t2)).isGreaterThan(Duration.ofSeconds(29));
        assertThat(Duration.between(OffsetDateTime.now(), t3)).isGreaterThan(Duration.ofSeconds(59));
    }

    @Test
    void capsAtOneHour() {
        var t = BackoffPolicy.nextRetryTime(99);
        Duration wait = Duration.between(OffsetDateTime.now(), t);
        assertThat(wait).isLessThanOrEqualTo(Duration.ofSeconds(3600));
        assertThat(wait).isGreaterThan(Duration.ofSeconds(3550));
    }
}
