package com.iam.server.security;

import java.io.IOException;
import java.time.Duration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.iam.server.service.RateLimitingService;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofSeconds(60);

    private final RateLimitingService rateLimitingService;

    public RateLimitingFilter(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Enforce rate limiting on sensitive authentication endpoints
        if (isRateLimitedPath(path)) {
            String clientIp = extractClientIp(request);
            String key = "auth:" + clientIp;

            boolean allowed = rateLimitingService.isAllowed(key, MAX_REQUESTS, WINDOW);
            long remaining = rateLimitingService.getRemainingRequests(key, MAX_REQUESTS);
            long resetSeconds = rateLimitingService.getResetSeconds(key, WINDOW);

            response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.setHeader("X-RateLimit-Reset", String.valueOf(resetSeconds));

            if (!allowed) {
                response.setStatus(429); // 429 Too Many Requests
                response.setHeader("Retry-After", String.valueOf(resetSeconds));
                response.setContentType("application/json");
                response.getWriter().write(String.format(
                        "{\"error\": \"Too many requests. Rate limit exceeded.\", \"retryAfterSeconds\": %d}",
                        resetSeconds
                ));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedPath(String path) {
        if (path == null) {
            return false;
        }
        return path.equals("/api/auth/login") ||
               path.equals("/api/auth/mfa/verify-login") ||
               path.startsWith("/api/auth/password-reset/");
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null) ? remoteAddr : "unknown";
    }
}
