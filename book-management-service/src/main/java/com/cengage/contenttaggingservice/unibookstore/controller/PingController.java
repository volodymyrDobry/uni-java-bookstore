package com.cengage.contenttaggingservice.unibookstore.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public, unauthenticated endpoint (permitted by /api/public/** in SecurityConfig)
 * used by the Lab 7 load generator. Because it passes security, requests reach the
 * ObservabilityFilter, so FAIL_RATE fault injection and the JSON access log apply —
 * which is what drives the metric-based auto-rollback demo.
 */
@RestController
public class PingController {

    private final String appVersion;

    public PingController(@Value("${APP_VERSION:dev}") String appVersion) {
        this.appVersion = appVersion;
    }

    @GetMapping("/api/public/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "version", appVersion);
    }
}
