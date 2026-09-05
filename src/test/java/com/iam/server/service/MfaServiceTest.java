package com.iam.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.iam.server.dto.MfaChallengeResponse;
import com.iam.server.dto.MfaSetupResponse;
import com.iam.server.entity.User;
import com.iam.server.repository.UserRepository;

class MfaServiceTest {

    private UserRepository userRepository;
    private TotpService totpService;
    private OtpDeliveryService otpDeliveryService;
    private MfaServiceImpl mfaService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        totpService = mock(TotpService.class);
        otpDeliveryService = mock(OtpDeliveryService.class);

        mfaService = new MfaServiceImpl(userRepository, totpService, otpDeliveryService);
    }

    @Test
    void testSetupMfa_TOTP_shouldReturnSecretAndQr() {
        User user = new User("alice", "pass");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(totpService.generateSecretKey()).thenReturn("JBSWY3DPEHPK3PXP");
        when(totpService.generateQrCodeUri(eq("alice"), anyString())).thenReturn("otpauth://totp/IAM-Server:alice?secret=JBSWY3DPEHPK3PXP");

        MfaSetupResponse response = mfaService.setupMfa("alice", "TOTP");

        assertNotNull(response);
        assertEquals("JBSWY3DPEHPK3PXP", response.getSecretKey());
        assertNotNull(response.getQrCodeUri());
        assertEquals("TOTP", response.getMfaType());
    }

    @Test
    void testEnableMfa_withValidTotp_shouldEnableMfa() {
        User user = new User("alice", "pass");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(totpService.generateSecretKey()).thenReturn("JBSWY3DPEHPK3PXP");
        when(totpService.verifyCode(eq("JBSWY3DPEHPK3PXP"), eq(123456))).thenReturn(true);

        mfaService.setupMfa("alice", "TOTP");
        boolean enabled = mfaService.enableMfa("alice", "123456", "TOTP", "alice@example.com", null);

        assertTrue(enabled);
        assertTrue(user.isMfaEnabled());
        assertEquals("TOTP", user.getMfaType());
        assertEquals("JBSWY3DPEHPK3PXP", user.getTotpSecret());
        verify(userRepository, atLeastOnce()).save(user);
    }

    @Test
    void testEnableMfa_withInvalidTotp_shouldReturnFalse() {
        User user = new User("alice", "pass");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(totpService.generateSecretKey()).thenReturn("JBSWY3DPEHPK3PXP");
        when(totpService.verifyCode(eq("JBSWY3DPEHPK3PXP"), eq(999999))).thenReturn(false);

        mfaService.setupMfa("alice", "TOTP");
        boolean enabled = mfaService.enableMfa("alice", "999999", "TOTP", null, null);

        assertFalse(enabled);
        assertFalse(user.isMfaEnabled());
    }

    @Test
    void testDisableMfa_shouldResetMfaFlags() {
        User user = new User("alice", "pass");
        user.setMfaEnabled(true);
        user.setMfaType("TOTP");
        user.setTotpSecret("SECRET");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        boolean disabled = mfaService.disableMfa("alice");

        assertTrue(disabled);
        assertFalse(user.isMfaEnabled());
        assertEquals("NONE", user.getMfaType());
        assertNull(user.getTotpSecret());
        verify(userRepository).save(user);
    }

    @Test
    void testInitiateLoginChallenge_shouldReturnTempToken() {
        User user = new User("alice", "pass");
        user.setMfaEnabled(true);
        user.setMfaType("TOTP");

        MfaChallengeResponse challenge = mfaService.initiateLoginChallenge(user);

        assertNotNull(challenge);
        assertTrue(challenge.isMfaRequired());
        assertNotNull(challenge.getTempToken());
        assertEquals("TOTP", challenge.getMfaType());
    }

    @Test
    void testVerifyLoginChallenge_withValidCode_shouldReturnUsername() {
        User user = new User("alice", "pass");
        user.setMfaEnabled(true);
        user.setMfaType("TOTP");
        user.setTotpSecret("SECRET");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(totpService.verifyCode("SECRET", 123456)).thenReturn(true);

        MfaChallengeResponse challenge = mfaService.initiateLoginChallenge(user);
        String username = mfaService.verifyLoginChallenge(challenge.getTempToken(), "123456");

        assertEquals("alice", username);
    }
}
