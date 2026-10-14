package com.taskflow.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deployment platforms like Render provide DATABASE_URL in `postgres://`
 * format. The JDBC driver needs `jdbc:postgresql://`, and the credentials
 * arrive embedded in the URL. This post-processor normalizes both, without
 * touching URLs that are already in jdbc: form.
 */
public class DatabaseUrlPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        String url = env.getProperty("spring.datasource.url");
        if (url == null || url.startsWith("jdbc:")) {
            return;
        }
        String normalized;
        int schemeEnd = url.indexOf("://");
        String scheme = schemeEnd > 0 ? url.substring(0, schemeEnd) : url;
        switch (scheme) {
            case "postgres", "postgresql" -> normalized = "jdbc:postgresql://" + url.substring(schemeEnd + 3);
            case "mysql" -> normalized = "jdbc:mysql://" + url.substring(schemeEnd + 3);
            default -> { return; }
        }

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("spring.datasource.url", normalized);

        String userInfo = null;
        String rest = normalized.substring("jdbc:postgresql://".length());
        int at = rest.lastIndexOf('@');
        if (at > 0) {
            userInfo = rest.substring(0, at);
            props.put("spring.datasource.url", "jdbc:postgresql://" + rest.substring(at + 1));
        }
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            if (colon > 0) {
                props.put("spring.datasource.username", userInfo.substring(0, colon));
                props.put("spring.datasource.password", userInfo.substring(colon + 1));
            } else {
                props.put("spring.datasource.username", userInfo);
            }
        }
        env.getPropertySources().addFirst(new MapPropertySource("render-database-url", props));
    }
}
