package com.iam.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis-backed distributed session and OAuth2 short-lived token store.
 * Handles storage and validation for:
 * 1. Short-lived OAuth2 Authorization Codes (5-minute TTL)
 * 2. OAuth2 Refresh Tokens (30-day TTL)
 * 3. User session tracking
 * (Zaalima Week 3 Project 3 Requirement)
 */
@Service
public class RedisOAuth2SessionService {

    private static final Logger logger = LoggerFactory.getLogger(RedisOAuth2SessionService.class);

    private static final String AUTH_CODE_PREFIX = "iam:oauth2:code:";
    private static final String REFRESH_TOKEN_PREFIX = "iam:oauth2:refresh:";
    private static final String OTP_CACHE_PREFIX = "iam:mfa:otp:";

    private final StringRedisTemplate redisTemplate;

    // Resilient fallback storage for standalone/local development
    private final Map<String, CodeEntry> localCodeStore = new ConcurrentHashMap<>();
    private final Map<String, TokenEntry> localRefreshStore = new ConcurrentHashMap<>();
    private final Map<String, CodeEntry> localOtpStore = new ConcurrentHashMap<>();

    private record CodeEntry(String username, String clientId, long expiresAt) {}
    private record TokenEntry(String username, long expiresAt) {}

    public RedisOAuth2SessionService(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Store short-lived authorization code (default 5 minutes).
     */
    public void storeAuthorizationCode(String code, String username, String clientId, Duration ttl) {
        if (ttl == null) {
            ttl = Duration.ofMinutes(5);
        }
        if (redisTemplate != null) {
            try {
                String payload = username + ":" + clientId;
                redisTemplate.opsForValue().set(AUTH_CODE_PREFIX + code, payload, ttl);
                logger.info("[REDIS SESSION] Stored authorization code in Redis with TTL {}s", ttl.toSeconds());
                return;
            } catch (Exception e) {
                logger.warn("[REDIS SESSION FALLBACK] Redis unavailable, using local store: {}", e.getMessage());
            }
        }
        localCodeStore.put(code, new CodeEntry(username, clientId, System.currentTimeMillis() + ttl.toMillis()));
    }

    /**
     * Consume and invalidate a one-time authorization code.
     */
    public boolean consumeAuthorizationCode(String code, String expectedClientId) {
        if (redisTemplate != null) {
            try {
                String key = AUTH_CODE_PREFIX + code;
                String payload = redisTemplate.opsForValue().get(key);
                if (payload != null) {
                    redisTemplate.delete(key);
                    String[] parts = payload.split(":");
                    return parts.length == 2 && parts[1].equals(expectedClientId);
                }
            } catch (Exception e) {
                logger.warn("[REDIS SESSION FALLBACK] Redis check failed: {}", e.getMessage());
            }
        }
        CodeEntry entry = localCodeStore.remove(code);
        return entry != null && entry.expiresAt() > System.currentTimeMillis() && entry.clientId().equals(expectedClientId);
    }

    /**
     * Store refresh token with extended TTL (e.g. 30 days).
     */
    public void storeRefreshToken(String refreshToken, String username, Duration ttl) {
        if (ttl == null) {
            ttl = Duration.ofDays(30);
        }
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + refreshToken, username, ttl);
                logger.info("[REDIS SESSION] Stored refresh token for [{}] with TTL {}d", username, ttl.toDays());
                return;
            } catch (Exception e) {
                logger.warn("[REDIS SESSION FALLBACK] Redis refresh token store failed: {}", e.getMessage());
            }
        }
        localRefreshStore.put(refreshToken, new TokenEntry(username, System.currentTimeMillis() + ttl.toMillis()));
    }

    /**
     * Verify if refresh token is currently valid and active.
     */
    public boolean isRefreshTokenValid(String refreshToken) {
        if (redisTemplate != null) {
            try {
                Boolean hasKey = redisTemplate.hasKey(REFRESH_TOKEN_PREFIX + refreshToken);
                if (Boolean.TRUE.equals(hasKey)) {
                    return true;
                }
            } catch (Exception e) {
                logger.warn("[REDIS SESSION FALLBACK] Redis check failed: {}", e.getMessage());
            }
        }
        TokenEntry entry = localRefreshStore.get(refreshToken);
        if (entry != null && entry.expiresAt() > System.currentTimeMillis()) {
            return true;
        }
        localRefreshStore.remove(refreshToken);
        return false;
    }

    /**
     * Revoke a refresh token.
     */
    public void revokeRefreshToken(String refreshToken) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(REFRESH_TOKEN_PREFIX + refreshToken);
                logger.info("[REDIS SESSION] Invalidated refresh token in Redis");
            } catch (Exception e) {
                logger.warn("[REDIS SESSION FALLBACK] Failed to delete refresh token from Redis: {}", e.getMessage());
            }
        }
        localRefreshStore.remove(refreshToken);
    }

    /**
     * Store a 6-digit MFA OTP code for delivery and verification (5-minute TTL).
     */
    public void storeMfaOtp(String destination, String otpCode) {
        Duration ttl = Duration.ofMinutes(5);
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(OTP_CACHE_PREFIX + destination, otpCode, ttl);
                return;
            } catch (Exception ignored) {}
        }
        localOtpStore.put(destination, new CodeEntry(otpCode, "MFA", System.currentTimeMillis() + ttl.toMillis()));
    }

    /**
     * Verify and consume a dispatched MFA OTP.
     */
    public boolean verifyMfaOtp(String destination, String otpCode) {
        if (redisTemplate != null) {
            try {
                String key = OTP_CACHE_PREFIX + destination;
                String cached = redisTemplate.opsForValue().get(key);
                if (cached != null && cached.equals(otpCode)) {
                    redisTemplate.delete(key);
                    return true;
                }
            } catch (Exception ignored) {}
        }
        CodeEntry entry = localOtpStore.get(destination);
        if (entry != null && entry.expiresAt() > System.currentTimeMillis() && entry.username().equals(otpCode)) {
            localOtpStore.remove(destination);
            return true;
        }
        return false;
    }
}
