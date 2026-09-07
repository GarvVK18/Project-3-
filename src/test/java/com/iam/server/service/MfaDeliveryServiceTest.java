package com.iam.server.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MfaDeliveryServiceTest {

    private MfaDeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        deliveryService = new MfaDeliveryService();
    }

    @Test
    void sendSmsOtp_shouldSucceedInFallbackMode() {
        boolean sent = deliveryService.sendSmsOtp("+1234567890", "123456");
        assertTrue(sent);
    }

    @Test
    void sendEmailOtp_shouldSucceedInFallbackMode() {
        boolean sent = deliveryService.sendEmailOtp("user@example.com", "654321");
        assertTrue(sent);
    }
}
