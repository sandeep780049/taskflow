package com.taskflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The production React dashboard is compiled into src/main/resources/static
 * and served by the same Spring Boot instance as the API.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private static final String[] ROUTES = {
            "/", "/login", "/register", "/dashboard", "/jobs", "/jobs/**", "/workers"
    };

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/jobs").setViewName("forward:/index.html");
        registry.addViewController("/workers").setViewName("forward:/index.html");
        // deep links like /jobs/<uuid>
        registry.addViewController("/jobs/{uuid:[0-9a-fA-F\\-]{36}}").setViewName("forward:/index.html");
    }
}
