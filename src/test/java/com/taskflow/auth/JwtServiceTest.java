package com.taskflow.auth;

import com.taskflow.user.Role;
import com.taskflow.user.UserAccount;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final String secret = "test-secret-key-that-is-long-enough-32!";

    @Test
    void roundTripsClaimsAndRejectsTampering() {
        var service = new JwtService(new JwtProperties(secret, 60));
        var user = new UserAccount("a@example.com", "hash", Role.USER);
        String token = service.issue(user);

        var claims = service.parse(token).orElseThrow();
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("email", String.class)).isEqualTo("a@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(service.isExpired(claims)).isFalse();

        assertThat(service.parse(token + "x")).isEmpty();
        assertThat(service.parse("garbage.token.value")).isEmpty();
    }

    @Test
    void rejectsShortSecrets() {
        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> new JwtProperties("short", 5))).isInstanceOf(IllegalStateException.class);
        new JwtProperties("a".repeat(32), 5);
    }
}
