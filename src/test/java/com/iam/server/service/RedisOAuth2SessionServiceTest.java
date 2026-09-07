package com.iam.server.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RedisOAuth2SessionServiceTest {

    private RedisOAuth2SessionService sessionService;

    @BeforeEach
    void setUp() {
        // Test in fallback/local mode (null RedisTemplate)
        sessionService = new RedisOAuth2SessionService(null);
    }

    @Test
    void authorizationCode_lifecycle_storesAndConsumesSuccessfully() {
        sessionService.storeAuthorizationCode("code-123", "pranav", "client-app", Duration.ofMinutes(5));

        // Wrong client id fails
        assertFalse(sessionService.consumeAuthorizationCode("code-123", "wrong-client"));

        // Correct client id succeeds
        sessionService.storeAuthorizationCode("code-456", "pranav", "client-app", Duration.ofMinutes(5));
        assertTrue(sessionService.consumeAuthorizationCode("code-456", "client-app"));

        // One-time use: second consumption must fail
        assertFalse(sessionService.consumeAuthorizationCode("code-456", "client-app"));
    }

    @Test
    void refreshToken_lifecycle_storesValidatesAndRevokes() {
        sessionService.storeRefreshToken("refresh-abc", "pranav", Duration.ofDays(30));

        assertTrue(sessionService.isRefreshTokenValid("refresh-abc"));

        sessionService.revokeRefreshToken("refresh-abc");

        assertFalse(sessionService.isRefreshTokenValid("refresh-abc"));
    }

    @Test
    void mfaOtp_lifecycle_storesAndVerifiesSuccessfully() {
        sessionService.storeMfaOtp("+1987654321", "889900");

        // Wrong code fails
        assertFalse(sessionService.verifyMfaOtp("+1987654321", "111111"));

        // Correct code succeeds
        assertTrue(sessionService.verifyMfaOtp("+1987654321", "889900"));

        // Consumed once
        assertFalse(sessionService.verifyMfaOtp("+1987654321", "889900"));
    }
}
