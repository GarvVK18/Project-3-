package com.iam.server.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    public static final int MAX_REQUESTS = 5;
    public static final int WINDOW_SECONDS = 60;
    private static final String REDIS_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;

    // Resilient in-memory fallback for local dev / tests when Redis is offline
    private final Map<String, Bucket> fallbackBuckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (isRateLimitedPath(path)) {
            String clientIp = resolveClientIp(request);
            boolean allowed = checkRateLimit(clientIp, response);
            if (!allowed) {
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean checkRateLimit(String clientIp, HttpServletResponse response) throws IOException {
        if (redisTemplate != null) {
            try {
                String key = REDIS_PREFIX + clientIp;
                Long count = redisTemplate.opsForValue().increment(key);
                if (count != null && count == 1) {
                    redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
                }

                Long ttl = redisTemplate.getExpire(key);
                long resetSeconds = (ttl != null && ttl > 0) ? ttl : WINDOW_SECONDS;

                response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
                response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, MAX_REQUESTS - (count != null ? count : 0))));
                response.setHeader("X-RateLimit-Reset", String.valueOf(resetSeconds));

                if (count != null && count > MAX_REQUESTS) {
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setHeader("Retry-After", String.valueOf(resetSeconds));
                    response.getWriter().write(String.format(
                            "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again in %d seconds.\"}",
                            resetSeconds));
                    return false;
                }
                return true;
            } catch (Exception e) {
                logger.warn("[RATE LIMIT REDIS FALLBACK] Falling back to in-memory rate limiter: {}", e.getMessage());
            }
        }

        // In-memory token-bucket fallback
        Bucket bucket = fallbackBuckets.computeIfAbsent(clientIp, k -> createFallbackBucket());
        long availableTokens = bucket.getAvailableTokens();
        response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, availableTokens - 1)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(WINDOW_SECONDS));

        if (!bucket.tryConsume(1)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(WINDOW_SECONDS));
            response.getWriter().write(String.format(
                    "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again in %d seconds.\"}",
                    WINDOW_SECONDS));
            return false;
        }

        return true;
    }

    private boolean isRateLimitedPath(String path) {
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/password-reset") ||
               path.startsWith("/api/mfa");
    }

    private Bucket createFallbackBucket() {
        Refill refill = Refill.greedy(MAX_REQUESTS, Duration.ofSeconds(WINDOW_SECONDS));
        Bandwidth limit = Bandwidth.classic(MAX_REQUESTS, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
