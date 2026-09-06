package com.iam.server.controller;

import com.iam.server.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TokenRevocationControllerTest {

    private TokenBlacklistService tokenBlacklistService;
    private TokenRevocationController controller;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = mock(TokenBlacklistService.class);
        controller = new TokenRevocationController(tokenBlacklistService);
    }

    @Test
    void revokeToken_withBearerHeader_shouldBlacklistToken() {
        String authHeader = "Bearer sample.token.to.revoke";

        ResponseEntity<?> response = controller.revokeToken(authHeader, null);

        assertEquals(200, response.getStatusCode().value());
        verify(tokenBlacklistService).blacklistToken(eq("sample.token.to.revoke"), anyLong());
    }

    @Test
    void revokeToken_withoutToken_shouldReturnBadRequest() {
        ResponseEntity<?> response = controller.revokeToken(null, null);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void logout_withAllDevices_shouldRevokeUserTokens() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("pranav");

        ResponseEntity<?> response = controller.logout("Bearer test.token", true, auth);

        assertEquals(200, response.getStatusCode().value());
        verify(tokenBlacklistService).blacklistToken(eq("test.token"), anyLong());
        verify(tokenBlacklistService).revokeUserTokens("pranav");
    }
}
