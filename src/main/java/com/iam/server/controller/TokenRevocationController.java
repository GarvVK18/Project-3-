package com.iam.server.controller;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.iam.server.service.TokenRevocationService;

@RestController
public class TokenRevocationController {

    private final TokenRevocationService tokenRevocationService;

    public TokenRevocationController(TokenRevocationService tokenRevocationService) {
        this.tokenRevocationService = tokenRevocationService;
    }

    @PostMapping("/api/auth/revoke")
    public ResponseEntity<?> revokeToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, String> body) {

        String token = null;
        if (authHeader != null && (authHeader.startsWith("Bearer ") || authHeader.startsWith("bearer "))) {
            token = authHeader.substring(7).trim();
        } else if (body != null && body.containsKey("token")) {
            token = body.get("token");
        }

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token is required to revoke"));
        }

        tokenRevocationService.revokeToken(token);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Token revoked successfully"));
    }

    @PostMapping("/api/auth/logout-all")
    public ResponseEntity<?> logoutAll(
            Principal principal,
            @RequestBody(required = false) Map<String, String> body) {

        String username = (principal != null) ? principal.getName() : null;
        if (username == null && body != null) {
            username = body.get("username");
        }

        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username or active authenticated session required"));
        }

        tokenRevocationService.forceLogoutUser(username);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Successfully logged out from all devices for user: " + username
        ));
    }

    @PostMapping("/api/admin/users/{username}/force-logout")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adminForceLogout(@PathVariable String username) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }

        tokenRevocationService.forceLogoutUser(username);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Admin successfully forced logout across all devices for user: " + username
        ));
    }
}
