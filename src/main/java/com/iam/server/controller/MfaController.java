package com.iam.server.controller;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.iam.server.dto.MfaEnableRequest;
import com.iam.server.dto.MfaLoginVerificationRequest;
import com.iam.server.dto.MfaSendOtpRequest;
import com.iam.server.dto.MfaSetupResponse;
import com.iam.server.security.JwtUtil;
import com.iam.server.service.MfaService;

@RestController
@RequestMapping("/api/auth/mfa")
public class MfaController {

    private final MfaService mfaService;
    private final JwtUtil jwtUtil;

    public MfaController(MfaService mfaService, JwtUtil jwtUtil) {
        this.mfaService = mfaService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/setup")
    public ResponseEntity<?> setupMfa(
            @RequestParam(required = false, defaultValue = "TOTP") String type,
            Principal principal,
            @RequestBody(required = false) Map<String, String> body) {

        String username = (principal != null) ? principal.getName() : null;
        if (username == null && body != null) {
            username = body.get("username");
        }

        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required to setup MFA"));
        }

        try {
            MfaSetupResponse response = mfaService.setupMfa(username, type);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/enable")
    public ResponseEntity<?> enableMfa(
            @RequestBody MfaEnableRequest request,
            Principal principal) {

        String username = (principal != null) ? principal.getName() : request.getUsername();
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }

        if (request.getCode() == null || request.getCode().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Verification code is required"));
        }

        boolean enabled = mfaService.enableMfa(
                username,
                request.getCode(),
                request.getMfaType(),
                request.getEmail(),
                request.getPhoneNumber()
        );

        if (enabled) {
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Multi-Factor Authentication enabled successfully",
                    "mfaType", (request.getMfaType() != null ? request.getMfaType().toUpperCase() : "TOTP")
            ));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid MFA verification code"));
    }

    @PostMapping("/disable")
    public ResponseEntity<?> disableMfa(
            Principal principal,
            @RequestBody(required = false) Map<String, String> body) {

        String username = (principal != null) ? principal.getName() : null;
        if (username == null && body != null) {
            username = body.get("username");
        }

        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }

        mfaService.disableMfa(username);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Multi-Factor Authentication disabled successfully"));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody MfaSendOtpRequest request) {
        if (request.getTempToken() == null || request.getTempToken().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Challenge tempToken is required"));
        }

        boolean sent = mfaService.sendOtpForChallenge(request.getTempToken());
        if (sent) {
            return ResponseEntity.ok(Map.of("message", "OTP resent successfully"));
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired challenge token"));
    }

    @PostMapping("/verify-login")
    public ResponseEntity<?> verifyLogin(@RequestBody MfaLoginVerificationRequest request) {
        if (request.getTempToken() == null || request.getCode() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "tempToken and code are required"));
        }

        String username = mfaService.verifyLoginChallenge(request.getTempToken(), request.getCode());
        if (username != null) {
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "MFA verification successful",
                    "token", token,
                    "username", username
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired MFA code"));
    }
}
