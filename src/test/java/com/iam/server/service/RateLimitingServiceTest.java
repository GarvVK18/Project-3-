package com.iam.server.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingServiceImpl();
    }

    @Test
    void testRateLimiting_allowsWithinLimit() {
        String key = "test-client-ip";
        int maxRequests = 3;
        Duration window = Duration.ofSeconds(10);

        assertTrue(rateLimitingService.isAllowed(key, maxRequests, window));
        assertTrue(rateLimitingService.isAllowed(key, maxRequests, window));
        assertTrue(rateLimitingService.isAllowed(key, maxRequests, window));

        // 4th request exceeds max 3
        assertFalse(rateLimitingService.isAllowed(key, maxRequests, window), "4th attempt must be rejected");
    }

    @Test
    void testGetRemainingRequests_decrementsCorrectly() {
        String key = "test-client-ip-2";
        int maxRequests = 5;
        Duration window = Duration.ofSeconds(10);

        assertEquals(5, rateLimitingService.getRemainingRequests(key, maxRequests));

        rateLimitingService.isAllowed(key, maxRequests, window);
        assertEquals(4, rateLimitingService.getRemainingRequests(key, maxRequests));

        rateLimitingService.isAllowed(key, maxRequests, window);
        assertEquals(3, rateLimitingService.getRemainingRequests(key, maxRequests));
    }

    @Test
    void testGetResetSeconds_returnsPositiveDuration() {
        String key = "test-client-ip-3";
        Duration window = Duration.ofSeconds(30);

        rateLimitingService.isAllowed(key, 5, window);
        long resetSeconds = rateLimitingService.getResetSeconds(key, window);

        assertTrue(resetSeconds > 0 && resetSeconds <= 30);
    }
}
