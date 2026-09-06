package com.iam.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TotpServiceTest {

    private TotpService totpService;

    @BeforeEach
    void setUp() {
        totpService = new TotpService();
    }

    @Test
    void generateSecretKey_shouldReturnValidBase32String() {
        String secret = totpService.generateSecretKey();
        assertNotNull(secret);
        assertTrue(secret.length() >= 16);
        assertTrue(secret.matches("^[A-Z2-7]+$"));
    }

    @Test
    void getQrCodeUri_shouldContainIssuerAndSecret() {
        String secret = "JBSWY3DPEHPK3PXP";
        String uri = totpService.getQrCodeUri("testuser", secret, "IAM-Server");

        assertNotNull(uri);
        assertTrue(uri.startsWith("otpauth://totp/"));
        assertTrue(uri.contains("secret=JBSWY3DPEHPK3PXP"));
        assertTrue(uri.contains("testuser"));
    }

    @Test
    void verifyCode_shouldValidateCurrentCode() {
        String secret = totpService.generateSecretKey();
        long now = System.currentTimeMillis() / 1000;
        int currentCode = totpService.generateCode(secret, now);

        assertTrue(totpService.verifyCode(secret, currentCode));
    }

    @Test
    void verifyCode_shouldRejectInvalidCode() {
        String secret = totpService.generateSecretKey();
        assertFalse(totpService.verifyCode(secret, 99999999));
    }
}
