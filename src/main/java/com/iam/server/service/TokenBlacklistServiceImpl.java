package com.iam.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistServiceImpl.class);
    private static final String TOKEN_PREFIX = "iam:blacklist:token:";
    private static final String USER_PREFIX = "iam:blacklist:user:";

    private final StringRedisTemplate redisTemplate;
    // Resilient in-memory fallback cache when Redis is unavailable or offline
    private final Map<String, Long> inMemoryBlacklist = new ConcurrentHashMap<>();
    private final Map<String, Long> inMemoryUserRevocations = new ConcurrentHashMap<>();

    public TokenBlacklistServiceImpl(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void blacklistToken(String token, long expirationMillis) {
        long ttlMillis = Math.max(expirationMillis, 3600_000L); // default at least 1 hr
        boolean redisUsed = false;

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(TOKEN_PREFIX + token, "revoked", Duration.ofMillis(ttlMillis));
                redisUsed = true;
                logger.info("[REDIS CACHE] Successfully blacklisted token in Redis with TTL {} ms", ttlMillis);
            } catch (Exception e) {
                logger.warn("[REDIS FALLBACK] Failed to reach Redis, falling back to in-memory blacklist: {}", e.getMessage());
            }
        }

        if (!redisUsed) {
            inMemoryBlacklist.put(token, System.currentTimeMillis() + ttlMillis);
            logger.info("[IN-MEMORY CACHE] Blacklisted token in local memory store");
        }
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        if (redisTemplate != null) {
            try {
                Boolean hasKey = redisTemplate.hasKey(TOKEN_PREFIX + token);
                if (Boolean.TRUE.equals(hasKey)) {
                    return true;
                }
            } catch (Exception e) {
                logger.warn("[REDIS FALLBACK] Error reading from Redis, checking local store: {}", e.getMessage());
            }
        }

        Long expiry = inMemoryBlacklist.get(token);
        if (expiry != null) {
            if (System.currentTimeMillis() < expiry) {
                return true;
            } else {
                inMemoryBlacklist.remove(token);
            }
        }

        return false;
    }

    @Override
    public void revokeUserTokens(String username) {
        long revocationTimestamp = System.currentTimeMillis();
        boolean redisUsed = false;

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(USER_PREFIX + username, String.valueOf(revocationTimestamp), Duration.ofDays(7));
                redisUsed = true;
                logger.info("[REDIS CACHE] Revoked all active tokens for user [{}] across all devices", username);
            } catch (Exception e) {
                logger.warn("[REDIS FALLBACK] Failed to record user revocation in Redis: {}", e.getMessage());
            }
        }

        if (!redisUsed) {
            inMemoryUserRevocations.put(username, revocationTimestamp);
            logger.info("[IN-MEMORY CACHE] Recorded token revocation for user [{}] in local memory store", username);
        }
    }

    @Override
    public boolean isUserRevoked(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        if (redisTemplate != null) {
            try {
                String value = redisTemplate.opsForValue().get(USER_PREFIX + username);
                if (value != null) {
                    return true;
                }
            } catch (Exception e) {
                logger.warn("[REDIS FALLBACK] Error checking user revocation in Redis: {}", e.getMessage());
            }
        }

        return inMemoryUserRevocations.containsKey(username);
    }
}
