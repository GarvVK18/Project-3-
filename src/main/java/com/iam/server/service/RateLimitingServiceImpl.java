package com.iam.server.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RateLimitingServiceImpl implements RateLimitingService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingServiceImpl.class);

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // Resilient in-memory fallback for local dev / testing if Redis is offline
    private final Map<String, InMemoryCounter> inMemoryCounters = new ConcurrentHashMap<>();

    private static class InMemoryCounter {
        final AtomicInteger count = new AtomicInteger(0);
        final Instant expiresAt;

        InMemoryCounter(Duration window) {
            this.expiresAt = Instant.now().plus(window);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    @Override
    public boolean isAllowed(String key, int maxRequests, Duration window) {
        String redisKey = "rate_limit:" + key;

        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                Long currentCount = redisTemplate.opsForValue().increment(redisKey);
                if (currentCount != null && currentCount == 1L) {
                    redisTemplate.expire(redisKey, window);
                }
                return currentCount != null && currentCount <= maxRequests;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, using in-memory rate limiting fallback: {}", e.getMessage());
        }

        // In-memory fallback
        InMemoryCounter counter = inMemoryCounters.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                InMemoryCounter newCounter = new InMemoryCounter(window);
                newCounter.count.incrementAndGet();
                return newCounter;
            } else {
                existing.count.incrementAndGet();
                return existing;
            }
        });

        return counter.count.get() <= maxRequests;
    }

    @Override
    public long getRemainingRequests(String key, int maxRequests) {
        String redisKey = "rate_limit:" + key;

        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                String val = redisTemplate.opsForValue().get(redisKey);
                if (val != null) {
                    long used = Long.parseLong(val);
                    return Math.max(0, maxRequests - used);
                }
                return maxRequests;
            }
        } catch (Exception ignored) {
        }

        InMemoryCounter counter = inMemoryCounters.get(key);
        if (counter != null && !counter.isExpired()) {
            return Math.max(0, maxRequests - counter.count.get());
        }

        return maxRequests;
    }

    @Override
    public long getResetSeconds(String key, Duration window) {
        String redisKey = "rate_limit:" + key;

        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                Long ttl = redisTemplate.getExpire(redisKey);
                if (ttl != null && ttl > 0) {
                    return ttl;
                }
            }
        } catch (Exception ignored) {
        }

        InMemoryCounter counter = inMemoryCounters.get(key);
        if (counter != null && !counter.isExpired()) {
            return Math.max(1, Duration.between(Instant.now(), counter.expiresAt).toSeconds());
        }

        return window.toSeconds();
    }
}
