package com.iam.server.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TotpServiceTest {

    private TotpService totpService;

    @BeforeEach
    void setUp() {
        totpService = new TotpServiceImpl();
    }

    @Test
    void testGenerateSecretKey_shouldReturnValidBase32Key() {
        String secret = totpService.generateSecretKey();

        assertNotNull(secret);
        assertTrue(secret.length() >= 16);
        assertTrue(secret.matches("^[A-Z2-7]+$"), "Secret must only contain Base32 characters");
    }

    @Test
    void testGenerateQrCodeUri_shouldContainAccountAndIssuer() {
        String secret = totpService.generateSecretKey();
        String uri = totpService.generateQrCodeUri("john_doe", secret);

        assertNotNull(uri);
        assertTrue(uri.startsWith("otpauth://totp/"));
        assertTrue(uri.contains("secret=" + secret));
        assertTrue(uri.contains("issuer=IAM-Server"));
    }

    @Test
    void testVerifyCode_withCurrentCode_shouldReturnTrue() {
        String secret = totpService.generateSecretKey();
        int currentCode = totpService.generateCurrentCode(secret);

        boolean isValid = totpService.verifyCode(secret, currentCode);
        assertTrue(isValid, "Current generated code must be valid");
    }

    @Test
    void testVerifyCode_withInvalidCode_shouldReturnFalse() {
        String secret = totpService.generateSecretKey();
        int currentCode = totpService.generateCurrentCode(secret);
        int wrongCode = (currentCode + 123456) % 1_000_000;

        boolean isValid = totpService.verifyCode(secret, wrongCode);
        assertFalse(isValid, "Incorrect code should not verify");
    }

    @Test
    void testVerifyCode_withBlankSecret_shouldReturnFalse() {
        assertFalse(totpService.verifyCode(null, 123456));
        assertFalse(totpService.verifyCode("", 123456));
    }
}
