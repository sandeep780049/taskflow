package com.taskflow.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
public class SpaFallbackController {

    private String indexHtml;

    @PostConstruct
    void loadIndex() throws Exception {
        ClassPathResource res = new ClassPathResource("static/index.html");
        indexHtml = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @RequestMapping(value = {"/", "/login", "/register", "/dashboard", "/jobs", "/workers",
            "/jobs/*", "/workers/*"},
            method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<String> spaFallback() {
        if (indexHtml == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("frontend not built");
        }
        return ResponseEntity.ok()
                .header("Content-Type", "text/html;charset=UTF-8")
                .body(indexHtml);
    }
}
