package com.iam.server.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtpDeliveryServiceTest {

    private OtpDeliveryService otpDeliveryService;

    @BeforeEach
    void setUp() {
        otpDeliveryService = new OtpDeliveryServiceImpl();
    }

    @Test
    void testGenerateOtp_shouldHaveCorrectLength() {
        String otp = otpDeliveryService.generateOtp(6);

        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("^\\d{6}$"), "OTP must contain 6 digits");
    }

    @Test
    void testStoreAndVerifyOtp_shouldReturnTrueForMatchingCode() {
        String key = "test-user";
        String otp = "123456";

        otpDeliveryService.storeOtp(key, otp, Duration.ofMinutes(5));

        boolean valid = otpDeliveryService.verifyStoredOtp(key, "123456");
        assertTrue(valid);

        // After successful verification, one-time token should be invalidated
        boolean reused = otpDeliveryService.verifyStoredOtp(key, "123456");
        assertFalse(reused, "OTP cannot be re-used after verification");
    }

    @Test
    void testVerifyStoredOtp_shouldReturnFalseForInvalidCode() {
        String key = "test-user-2";
        otpDeliveryService.storeOtp(key, "654321", Duration.ofMinutes(5));

        boolean valid = otpDeliveryService.verifyStoredOtp(key, "000000");
        assertFalse(valid);
    }

    @Test
    void testSendEmailOtp_withValidRecipient_shouldReturnTrue() {
        boolean sent = otpDeliveryService.sendEmailOtp("user@example.com", "123456");
        assertTrue(sent);
    }

    @Test
    void testSendEmailOtp_withEmptyRecipient_shouldReturnFalse() {
        boolean sent = otpDeliveryService.sendEmailOtp("", "123456");
        assertFalse(sent);
    }

    @Test
    void testSendSmsOtp_withValidRecipient_shouldReturnTrue() {
        boolean sent = otpDeliveryService.sendSmsOtp("+1234567890", "123456");
        assertTrue(sent);
    }

    @Test
    void testSendSmsOtp_withEmptyRecipient_shouldReturnFalse() {
        boolean sent = otpDeliveryService.sendSmsOtp("", "123456");
        assertFalse(sent);
    }
}
