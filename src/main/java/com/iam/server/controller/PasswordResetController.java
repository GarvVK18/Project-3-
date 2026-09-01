package com.iam.server.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iam.server.dto.PasswordResetConfirmRequest;
import com.iam.server.dto.PasswordResetRequest;
import com.iam.server.service.UserService;

@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {

    private final UserService userService;

    public PasswordResetController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Step 1: User requests password reset token for their username.
     * Note: In production, an email service layer would deliver this token via email.
     */
    @PostMapping("/request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        try {
            String resetToken = userService.createPasswordResetToken(request.getUsername());
            return ResponseEntity.ok(Map.of(
                "message", "Password reset token generated successfully. In production, this token would be sent via email.",
                "resetToken", resetToken
            ));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * Step 2: User submits reset token and new password to confirm reset.
     */
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPasswordReset(@RequestBody PasswordResetConfirmRequest request) {
        try {
            userService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
