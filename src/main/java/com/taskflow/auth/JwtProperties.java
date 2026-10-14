package com.taskflow.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "taskflow.jwt")
public record JwtProperties(String secret, int expiryMinutes) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("taskflow.jwt.secret must be at least 32 characters");
        }
    }
}
