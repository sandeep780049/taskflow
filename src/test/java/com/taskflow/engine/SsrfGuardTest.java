package com.taskflow.engine;

import com.taskflow.common.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class SsrfGuardTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:8080/api",
            "http://127.0.0.1/x",
            "http://[::1]/x",
            "http://169.254.169.254/latest/meta-data/",
            "http://10.0.0.1/x",
            "http://192.168.1.10/x",
            "http://172.16.0.5/x"
    })
    void blocksPrivateAndReservedAddresses(String url) {
        assertThatThrownBy(() -> SsrfGuard.validate(url)).isInstanceOf(ApiException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com/file",
            "file:///etc/passwd",
            "notaurl",
            "http:///nohost",
            "http://user:pass@example.com/x"
    })
    void blocksNonHttpAndCredentialEmbedding(String url) {
        assertThatThrownBy(() -> SsrfGuard.validate(url)).isInstanceOf(ApiException.class);
    }

    @Test
    void blocksLowPorts() {
        assertThatThrownBy(() -> SsrfGuard.validate("http://93.184.216.34:22/x")).isInstanceOf(ApiException.class);
    }

    @Test
    void acceptsPublicHttpsUrl() {
        var uri = SsrfGuard.validate("http://93.184.216.34:8443/status");
        assertThat(uri.getPort()).isEqualTo(8443);
    }

    @Test
    void resolvesAndAcceptsWellKnownPublicHost() {
        assertThatCode(() -> SsrfGuard.validate("https://github.com/status"))
                .doesNotThrowAnyException();
    }
}
