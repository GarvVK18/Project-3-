package com.iam.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.iam.server.service.TokenRevocationService;

class TokenRevocationControllerTest {

    private TokenRevocationService tokenRevocationService;
    private TokenRevocationController controller;

    @BeforeEach
    void setUp() {
        tokenRevocationService = mock(TokenRevocationService.class);
        controller = new TokenRevocationController(tokenRevocationService);
    }

    @Test
    void testRevokeToken_viaHeader_shouldCallService() {
        ResponseEntity<?> response = controller.revokeToken("Bearer my-secret-jwt-token", null);

        assertEquals(200, response.getStatusCode().value());
        verify(tokenRevocationService).revokeToken("my-secret-jwt-token");
    }

    @Test
    void testRevokeToken_viaBody_shouldCallService() {
        ResponseEntity<?> response = controller.revokeToken(null, Map.of("token", "raw-jwt-token"));

        assertEquals(200, response.getStatusCode().value());
        verify(tokenRevocationService).revokeToken("raw-jwt-token");
    }

    @Test
    void testRevokeToken_withoutToken_shouldReturnBadRequest() {
        ResponseEntity<?> response = controller.revokeToken(null, null);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void testLogoutAll_withPrincipal_shouldCallForceLogout() {
        Principal principal = () -> "alice";
        ResponseEntity<?> response = controller.logoutAll(principal, null);

        assertEquals(200, response.getStatusCode().value());
        verify(tokenRevocationService).forceLogoutUser("alice");
    }

    @Test
    void testAdminForceLogout_shouldCallService() {
        ResponseEntity<?> response = controller.adminForceLogout("charlie");

        assertEquals(200, response.getStatusCode().value());
        verify(tokenRevocationService).forceLogoutUser("charlie");
    }
}
