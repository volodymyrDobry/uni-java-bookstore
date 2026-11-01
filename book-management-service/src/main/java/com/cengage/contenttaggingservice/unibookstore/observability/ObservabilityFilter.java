package com.cengage.contenttaggingservice.unibookstore.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lab 7 observability for book-management-service:
 *  - structured JSON access log per request (method, route, status, duration, version),
 *  - optional fault injection controlled by FAIL_RATE (fraction of requests answered 500),
 *    used to demonstrate metric-based auto-rollback.
 *
 * HTTP metrics themselves come from Micrometer's built-in http_server_requests_*,
 * which also counts the injected 500s (this filter runs inside that metrics filter).
 */
@Component
public class ObservabilityFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger("access");
    // Low-cardinality: never log raw ids. Actuator/docs paths are not business traffic.
    private static final Set<String> SKIP_PREFIXES = Set.of("/actuator", "/swagger", "/v3/api-docs", "/favicon");

    private final double failRate;
    private final String appVersion;

    public ObservabilityFilter(@Value("${FAIL_RATE:0}") double failRate,
                               @Value("${APP_VERSION:dev}") String appVersion) {
        this.failRate = failRate;
        this.appVersion = appVersion;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return SKIP_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long startNanos = System.nanoTime();
        boolean injected = failRate > 0 && ThreadLocalRandom.current().nextDouble() < failRate;
        try {
            if (injected) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"injected failure\"}");
            } else {
                chain.doFilter(request, response);
            }
        } finally {
            logRequest(request, response, (System.nanoTime() - startNanos) / 1_000_000.0);
        }
    }

    private void logRequest(HttpServletRequest request, HttpServletResponse response, double durationMs) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String route = pattern != null ? pattern.toString() : "unknown";
        int status = response.getStatus();
        String level = status >= 500 ? "error" : "info";

        String json = "{"
                + "\"level\":\"" + level + "\","
                + "\"msg\":\"request\","
                + "\"method\":\"" + escape(request.getMethod()) + "\","
                + "\"route\":\"" + escape(route) + "\","
                + "\"status\":" + status + ","
                + "\"duration_ms\":" + (Math.round(durationMs * 10.0) / 10.0) + ","
                + "\"version\":\"" + escape(appVersion) + "\""
                + "}";

        if (status >= 500) {
            LOG.error(json);
        } else {
            LOG.info(json);
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
