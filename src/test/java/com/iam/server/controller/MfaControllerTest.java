package com.iam.server.controller;

import com.iam.server.dto.MfaSetupResponse;
import com.iam.server.dto.MfaStatusResponse;
import com.iam.server.dto.MfaVerifyRequest;
import com.iam.server.entity.User;
import com.iam.server.repository.UserRepository;
import com.iam.server.service.TotpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MfaControllerTest {

    private UserRepository userRepository;
    private TotpService totpService;
    private MfaController mfaController;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        totpService = mock(TotpService.class);
        mfaController = new MfaController(userRepository, totpService);
    }

    @Test
    void setupMfa_shouldReturnSecretAndQrUri() {
        User user = new User("pranav", "pass");
        when(userRepository.findByUsername("pranav")).thenReturn(Optional.of(user));
        when(totpService.generateSecretKey()).thenReturn("BASE32SECRET");
        when(totpService.getQrCodeUri(eq("pranav"), eq("BASE32SECRET"), anyString()))
                .thenReturn("otpauth://totp/IAM-Server:pranav?secret=BASE32SECRET");

        ResponseEntity<?> response = mfaController.setupMfa("pranav", null);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof MfaSetupResponse);
        MfaSetupResponse body = (MfaSetupResponse) response.getBody();
        assertEquals("BASE32SECRET", body.getSecret());
        verify(userRepository).save(user);
    }

    @Test
    void enableMfa_withValidCode_shouldEnableMfa() {
        User user = new User("pranav", "pass");
        user.setMfaSecret("BASE32SECRET");
        when(userRepository.findByUsername("pranav")).thenReturn(Optional.of(user));
        when(totpService.verifyCode("BASE32SECRET", 123456)).thenReturn(true);

        MfaVerifyRequest request = new MfaVerifyRequest("pranav", 123456);
        ResponseEntity<?> response = mfaController.enableMfa(request, null);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof MfaStatusResponse);
        MfaStatusResponse body = (MfaStatusResponse) response.getBody();
        assertTrue(body.isMfaEnabled());
        assertTrue(user.isMfaEnabled());
        verify(userRepository).save(user);
    }

    @Test
    void enableMfa_withInvalidCode_shouldReturnUnauthorized() {
        User user = new User("pranav", "pass");
        user.setMfaSecret("BASE32SECRET");
        when(userRepository.findByUsername("pranav")).thenReturn(Optional.of(user));
        when(totpService.verifyCode("BASE32SECRET", 999999)).thenReturn(false);

        MfaVerifyRequest request = new MfaVerifyRequest("pranav", 999999);
        ResponseEntity<?> response = mfaController.enableMfa(request, null);

        assertEquals(401, response.getStatusCode().value());
        assertFalse(user.isMfaEnabled());
    }

    @Test
    void disableMfa_shouldTurnOffMfa() {
        User user = new User("pranav", "pass");
        user.setMfaEnabled(true);
        user.setMfaSecret("BASE32SECRET");
        when(userRepository.findByUsername("pranav")).thenReturn(Optional.of(user));

        ResponseEntity<?> response = mfaController.disableMfa("pranav", null);

        assertEquals(200, response.getStatusCode().value());
        assertFalse(user.isMfaEnabled());
        assertNull(user.getMfaSecret());
        verify(userRepository).save(user);
    }

    @Test
    void getMfaStatus_shouldReturnCurrentStatus() {
        User user = new User("pranav", "pass");
        user.setMfaEnabled(true);
        when(userRepository.findByUsername("pranav")).thenReturn(Optional.of(user));

        ResponseEntity<?> response = mfaController.getMfaStatus("pranav", null);

        assertEquals(200, response.getStatusCode().value());
        MfaStatusResponse body = (MfaStatusResponse) response.getBody();
        assertTrue(body.isMfaEnabled());
    }
}
