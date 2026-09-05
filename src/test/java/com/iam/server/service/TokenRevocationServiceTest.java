package com.iam.server.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.iam.server.security.JwtUtil;

class TokenRevocationServiceTest {

    private JwtUtil jwtUtil;
    private TokenRevocationServiceImpl tokenRevocationService;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        tokenRevocationService = new TokenRevocationServiceImpl(jwtUtil);
    }

    @Test
    void testRevokeToken_shouldBlacklistSingleToken() {
        String token = jwtUtil.generateToken("alice");

        assertFalse(tokenRevocationService.isTokenRevoked(token), "Token should initially be valid");

        tokenRevocationService.revokeToken(token);

        assertTrue(tokenRevocationService.isTokenRevoked(token), "Token must be reported as revoked after blacklisting");
    }

    @Test
    void testForceLogoutUser_shouldRevokeAllPriorTokensForUser() throws InterruptedException {
        String token1 = jwtUtil.generateToken("bob");

        // Delay to cross second boundary (JWT iat resolution is in seconds)
        Thread.sleep(1100);

        // Force logout Bob from all devices
        tokenRevocationService.forceLogoutUser("bob");

        assertTrue(tokenRevocationService.isTokenRevoked(token1), "Tokens issued before force logout must be revoked");

        // Delay to cross second boundary for subsequent token
        Thread.sleep(1100);

        // New token issued after force-logout should be valid
        String token2 = jwtUtil.generateToken("bob");
        assertFalse(tokenRevocationService.isTokenRevoked(token2), "Token issued after force logout should be valid");
    }

    @Test
    void testIsTokenRevoked_withBlankToken_shouldReturnTrue() {
        assertTrue(tokenRevocationService.isTokenRevoked(null));
        assertTrue(tokenRevocationService.isTokenRevoked(""));
    }
}
