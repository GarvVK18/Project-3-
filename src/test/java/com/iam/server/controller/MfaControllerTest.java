package com.iam.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.iam.server.dto.MfaEnableRequest;
import com.iam.server.dto.MfaLoginVerificationRequest;
import com.iam.server.dto.MfaSetupResponse;
import com.iam.server.security.JwtUtil;
import com.iam.server.service.MfaService;

class MfaControllerTest {

    private MfaService mfaService;
    private JwtUtil jwtUtil;
    private MfaController mfaController;

    @BeforeEach
    void setUp() {
        mfaService = mock(MfaService.class);
        jwtUtil = mock(JwtUtil.class);
        mfaController = new MfaController(mfaService, jwtUtil);
    }

    @Test
    void testSetupMfa_withPrincipal_shouldReturnOk() {
        Principal principal = () -> "alice";
        MfaSetupResponse mockResponse = new MfaSetupResponse("SECRET", "URI", "TOTP", "Setup ok");
        when(mfaService.setupMfa("alice", "TOTP")).thenReturn(mockResponse);

        ResponseEntity<?> response = mfaController.setupMfa("TOTP", principal, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockResponse, response.getBody());
    }

    @Test
    void testEnableMfa_withValidCode_shouldReturnSuccess() {
        MfaEnableRequest request = new MfaEnableRequest();
        request.setUsername("alice");
        request.setCode("123456");
        request.setMfaType("TOTP");

        when(mfaService.enableMfa("alice", "123456", "TOTP", null, null)).thenReturn(true);

        ResponseEntity<?> response = mfaController.enableMfa(request, null);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().toString().contains("SUCCESS"));
    }

    @Test
    void testEnableMfa_withInvalidCode_shouldReturnBadRequest() {
        MfaEnableRequest request = new MfaEnableRequest();
        request.setUsername("alice");
        request.setCode("000000");

        when(mfaService.enableMfa(eq("alice"), eq("000000"), any(), any(), any())).thenReturn(false);

        ResponseEntity<?> response = mfaController.enableMfa(request, null);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testVerifyLogin_withValidCode_shouldReturnJwtToken() {
        MfaLoginVerificationRequest request = new MfaLoginVerificationRequest("temp-token-123", "123456");
        when(mfaService.verifyLoginChallenge("temp-token-123", "123456")).thenReturn("alice");
        when(jwtUtil.generateToken("alice")).thenReturn("jwt-token-xyz");

        ResponseEntity<?> response = mfaController.verifyLogin(request);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().toString().contains("jwt-token-xyz"));
    }

    @Test
    void testVerifyLogin_withInvalidCode_shouldReturnUnauthorized() {
        MfaLoginVerificationRequest request = new MfaLoginVerificationRequest("temp-token-123", "000000");
        when(mfaService.verifyLoginChallenge("temp-token-123", "000000")).thenReturn(null);

        ResponseEntity<?> response = mfaController.verifyLogin(request);

        assertEquals(401, response.getStatusCode().value());
    }
}
