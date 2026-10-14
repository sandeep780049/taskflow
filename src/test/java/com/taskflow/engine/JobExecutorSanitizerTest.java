package com.taskflow.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobExecutorSanitizerTest {

    @Test
    void masksSecretsInErrorMessages() {
        assertThat(JobExecutor.safeMessage("failed with api_key=sk-live-123 + extra"))
                .contains("api_key=***").doesNotContain("sk-live-123");
        assertThat(JobExecutor.safeMessage("Authorization: Bearer abc.def.ghi"))
                .contains("Authorization=***").doesNotContain("abc.def.ghi");
    }

    @Test
    void handlesNullAndTruncates() {
        assertThat(JobExecutor.safeMessage(null)).isEqualTo("Unknown handler error");
        assertThat(JobExecutor.truncate("x".repeat(3000))).hasSize(2000);
        assertThat(JobExecutor.truncate(null)).isNull();
    }
}
