package com.iam.server.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.iam.server.security.JwtUtil;

@Service
public class TokenRevocationServiceImpl implements TokenRevocationService {

    private static final Logger log = LoggerFactory.getLogger(TokenRevocationServiceImpl.class);

    private final JwtUtil jwtUtil;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // Resilient fallback storage if Redis is unavailable/offline
    private final Map<String, Instant> inMemoryBlacklist = new ConcurrentHashMap<>();
    private final Map<String, Long> inMemoryUserRevocations = new ConcurrentHashMap<>();

    public TokenRevocationServiceImpl(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        String cleanToken = cleanBearerToken(token);
        long remainingTtlSeconds = jwtUtil.getRemainingTtlSeconds(cleanToken);
        if (remainingTtlSeconds <= 0) {
            remainingTtlSeconds = 3600; // 1 hour safety default
        }

        String redisKey = "blacklist:token:" + cleanToken;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                redisTemplate.opsForValue().set(redisKey, "revoked", Duration.ofSeconds(remainingTtlSeconds));
                log.info("Blacklisted token in Redis for {} seconds", remainingTtlSeconds);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, blacklisting in memory fallback: {}", e.getMessage());
        }

        inMemoryBlacklist.put(cleanToken, Instant.now().plusSeconds(remainingTtlSeconds));
    }

    @Override
    public void forceLogoutUser(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        long nowSeconds = System.currentTimeMillis() / 1000L;
        String redisKey = "revocation:user:" + username;

        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                // Keep the revocation timestamp for 30 days (max token life)
                redisTemplate.opsForValue().set(redisKey, String.valueOf(nowSeconds), Duration.ofDays(30));
                log.info("Registered force-logout for user [{}] across all devices in Redis", username);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, storing force-logout in memory fallback: {}", e.getMessage());
        }

        inMemoryUserRevocations.put(username, nowSeconds);
    }

    @Override
    public boolean isTokenRevoked(String token) {
        if (token == null || token.isBlank()) {
            return true;
        }

        String cleanToken = cleanBearerToken(token);

        // 1. Check direct token blacklist in Redis
        String blacklistKey = "blacklist:token:" + cleanToken;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                String val = redisTemplate.opsForValue().get(blacklistKey);
                if (val != null) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, checking memory blacklist: {}", e.getMessage());
        }

        // Check fallback blacklist
        Instant expiry = inMemoryBlacklist.get(cleanToken);
        if (expiry != null) {
            if (Instant.now().isBefore(expiry)) {
                return true;
            } else {
                inMemoryBlacklist.remove(cleanToken);
            }
        }

        // 2. Check user-wide force logout
        String username = jwtUtil.extractUsername(cleanToken);
        Date issuedAt = jwtUtil.extractIssuedAt(cleanToken);

        if (username != null && issuedAt != null) {
            long tokenIssuedAtSeconds = issuedAt.getTime() / 1000L;
            String userRevocationKey = "revocation:user:" + username;

            try {
                if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                    String cutoffStr = redisTemplate.opsForValue().get(userRevocationKey);
                    if (cutoffStr != null) {
                        long cutoffSeconds = Long.parseLong(cutoffStr);
                        if (tokenIssuedAtSeconds <= cutoffSeconds) {
                            return true; // Token was issued at or before force-logout cutoff!
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Redis unavailable, checking memory user revocations: {}", e.getMessage());
            }

            Long inMemoryCutoff = inMemoryUserRevocations.get(username);
            if (inMemoryCutoff != null && tokenIssuedAtSeconds <= inMemoryCutoff) {
                return true;
            }
        }

        return false;
    }

    private String cleanBearerToken(String token) {
        if (token.startsWith("Bearer ") || token.startsWith("bearer ")) {
            return token.substring(7).trim();
        }
        return token.trim();
    }
}
