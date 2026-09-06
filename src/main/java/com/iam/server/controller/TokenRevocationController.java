package com.iam.server.controller;

import com.iam.server.service.TokenBlacklistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class TokenRevocationController {

    private final TokenBlacklistService tokenBlacklistService;

    public TokenRevocationController(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revokeToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, String> body) {

        String token = extractToken(authHeader, body);
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token to revoke is required"));
        }

        // Default 24-hour expiration for blacklist record
        tokenBlacklistService.blacklistToken(token, 86_400_000L);

        return ResponseEntity.ok(Map.of(
                "status", "REVOKED",
                "message", "Token has been successfully revoked and blacklisted."
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "allDevices", defaultValue = "false") boolean allDevices,
            Authentication authentication) {

        String token = extractToken(authHeader, null);
        if (token != null) {
            tokenBlacklistService.blacklistToken(token, 86_400_000L);
        }

        if (allDevices && authentication != null && authentication.isAuthenticated()) {
            tokenBlacklistService.revokeUserTokens(authentication.getName());
            return ResponseEntity.ok(Map.of(
                    "status", "LOGGED_OUT_ALL_DEVICES",
                    "message", "User logged out across all devices. All active sessions invalidated."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "status", "LOGGED_OUT",
                "message", "Current session terminated and token revoked."
        ));
    }

    private String extractToken(String authHeader, Map<String, String> body) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        if (body != null && body.containsKey("token")) {
            return body.get("token");
        }
        return null;
    }
}
