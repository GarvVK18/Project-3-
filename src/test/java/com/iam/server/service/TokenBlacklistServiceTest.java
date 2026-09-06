package com.iam.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBlacklistServiceTest {

    private TokenBlacklistServiceImpl tokenBlacklistService;

    @BeforeEach
    void setUp() {
        // Test in-memory fallback mode (null RedisTemplate)
        tokenBlacklistService = new TokenBlacklistServiceImpl(null);
    }

    @Test
    void blacklistToken_shouldRecognizeRevokedToken() {
        String token = "sample.jwt.token.here";

        assertFalse(tokenBlacklistService.isTokenBlacklisted(token));

        tokenBlacklistService.blacklistToken(token, 10_000);

        assertTrue(tokenBlacklistService.isTokenBlacklisted(token));
    }

    @Test
    void isTokenBlacklisted_withEmptyOrNull_shouldReturnFalse() {
        assertFalse(tokenBlacklistService.isTokenBlacklisted(null));
        assertFalse(tokenBlacklistService.isTokenBlacklisted(""));
    }

    @Test
    void revokeUserTokens_shouldMarkUserAsRevoked() {
        String username = "pranav";

        assertFalse(tokenBlacklistService.isUserRevoked(username));

        tokenBlacklistService.revokeUserTokens(username);

        assertTrue(tokenBlacklistService.isUserRevoked(username));
    }
}
