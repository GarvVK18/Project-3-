package com.iam.server.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.iam.server.dto.PasswordResetConfirmRequest;
import com.iam.server.dto.PasswordResetRequest;
import com.iam.server.service.UserService;

class PasswordResetControllerTest {

    private UserService userService;
    private PasswordResetController passwordResetController;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        passwordResetController = new PasswordResetController(userService);
    }

    @Test
    void requestPasswordReset_shouldReturnSuccessResponse() {
        PasswordResetRequest request = new PasswordResetRequest("pranav");

        when(userService.createPasswordResetToken("pranav"))
                .thenReturn("test-reset-token");

        ResponseEntity<?> response =
                passwordResetController.requestPasswordReset(request);

        assertEquals(200, response.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("test-reset-token", body.get("resetToken"));
        assertEquals(
                "Password reset token generated successfully. In production, this token would be sent via email.",
                body.get("message")
        );
    }

    @Test
    void requestPasswordReset_shouldReturnBadRequestWhenServiceFails() {
        PasswordResetRequest request = new PasswordResetRequest("unknown");

        when(userService.createPasswordResetToken("unknown"))
                .thenThrow(new RuntimeException("User not found"));

        ResponseEntity<?> response =
                passwordResetController.requestPasswordReset(request);

        assertEquals(400, response.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("User not found", body.get("error"));
    }

    @Test
    void confirmPasswordReset_shouldReturnSuccessResponse() {
        PasswordResetConfirmRequest request =
                new PasswordResetConfirmRequest("test-reset-token", "newPassword");

        ResponseEntity<?> response =
                passwordResetController.confirmPasswordReset(request);

        assertEquals(200, response.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(
                "Password has been reset successfully.",
                body.get("message")
        );
    }

    @Test
    void confirmPasswordReset_shouldReturnBadRequestWhenServiceFails() {
        PasswordResetConfirmRequest request =
                new PasswordResetConfirmRequest("invalid-token", "newPassword");

        doThrow(new IllegalArgumentException("Invalid or expired reset token"))
        .when(userService)
        .resetPassword("invalid-token", "newPassword");

        ResponseEntity<?> response =
                passwordResetController.confirmPasswordReset(request);

        assertEquals(400, response.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals(
                "Invalid or expired reset token",
                body.get("error")
        );
    }
}