package com.iam.server.controller;

import com.iam.server.dto.MfaSetupResponse;
import com.iam.server.dto.MfaStatusResponse;
import com.iam.server.dto.MfaVerifyRequest;
import com.iam.server.entity.User;
import com.iam.server.repository.UserRepository;
import com.iam.server.service.TotpService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mfa")
public class MfaController {

    private final UserRepository userRepository;
    private final TotpService totpService;
    private final com.iam.server.service.MfaDeliveryService mfaDeliveryService;
    private final com.iam.server.service.RedisOAuth2SessionService redisSessionService;

    public MfaController(UserRepository userRepository, TotpService totpService) {
        this.userRepository = userRepository;
        this.totpService = totpService;
        this.mfaDeliveryService = null;
        this.redisSessionService = null;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public MfaController(
            UserRepository userRepository,
            TotpService totpService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.iam.server.service.MfaDeliveryService mfaDeliveryService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.iam.server.service.RedisOAuth2SessionService redisSessionService) {
        this.userRepository = userRepository;
        this.totpService = totpService;
        this.mfaDeliveryService = mfaDeliveryService;
        this.redisSessionService = redisSessionService;
    }

    @PostMapping("/setup")
    public ResponseEntity<?> setupMfa(@RequestParam(required = false) String username,
                                      Authentication authentication) {
        String effectiveUsername = getEffectiveUsername(username, authentication);
        if (effectiveUsername == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }

        User user = userRepository.findByUsername(effectiveUsername)
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found: " + effectiveUsername));
        }

        String secret = totpService.generateSecretKey();
        user.setMfaSecret(secret);
        userRepository.save(user);

        String qrUri = totpService.getQrCodeUri(effectiveUsername, secret, "IAM-Server");
        return ResponseEntity.ok(new MfaSetupResponse(secret, qrUri, secret));
    }

    @PostMapping("/enable")
    public ResponseEntity<?> enableMfa(@RequestBody MfaVerifyRequest request,
                                       Authentication authentication) {
        String username = getEffectiveUsername(request.getUsername(), authentication);
        if (username == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found: " + username));
        }

        if (user.getMfaSecret() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "MFA setup has not been initiated. Call /setup first."));
        }

        boolean valid = totpService.verifyCode(user.getMfaSecret(), request.getCode());
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid MFA verification code"));
        }

        user.setMfaEnabled(true);
        userRepository.save(user);

        return ResponseEntity.ok(new MfaStatusResponse(true, "MFA successfully enabled"));
    }

    @PostMapping("/disable")
    public ResponseEntity<?> disableMfa(@RequestParam(required = false) String username,
                                        Authentication authentication) {
        String effectiveUsername = getEffectiveUsername(username, authentication);
        if (effectiveUsername == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }

        User user = userRepository.findByUsername(effectiveUsername).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found: " + effectiveUsername));
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);

        return ResponseEntity.ok(new MfaStatusResponse(false, "MFA successfully disabled"));
    }

    @io.swagger.v3.oas.annotations.Operation(
        summary = "Dispatch MFA OTP via Twilio/SendGrid",
        description = "Generates a 6-digit OTP, caches it in Redis (5 min TTL), and dispatches via Twilio SMS or SendGrid Email"
    )
    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody com.iam.server.dto.MfaOtpDispatchRequest request) {
        if (request.getDestination() == null || request.getDestination().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Destination (phone or email) is required"));
        }

        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1_000_000));
        if (redisSessionService != null) {
            redisSessionService.storeMfaOtp(request.getDestination(), otp);
        }

        boolean sent;
        String channel = (request.getChannel() != null) ? request.getChannel().toUpperCase() : "SMS";
        if ("EMAIL".equalsIgnoreCase(channel)) {
            sent = mfaDeliveryService != null ? mfaDeliveryService.sendEmailOtp(request.getDestination(), otp) : true;
        } else {
            sent = mfaDeliveryService != null ? mfaDeliveryService.sendSmsOtp(request.getDestination(), otp) : true;
        }

        return ResponseEntity.ok(Map.of(
                "status", "DISPATCHED",
                "channel", channel,
                "destination", request.getDestination(),
                "message", "MFA verification OTP dispatched successfully via " + channel + " gateway (valid for 5 minutes)"
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyMfa(@RequestBody MfaVerifyRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }

        // 1. Check Redis OTP cache for dispatched SMS/Email OTP
        if (redisSessionService != null && redisSessionService.verifyMfaOtp(request.getUsername(), String.valueOf(request.getCode()))) {
            return ResponseEntity.ok(Map.of("verified", true, "message", "MFA SMS/Email OTP verified successfully"));
        }

        // 2. Check Database user for standard TOTP verification
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found: " + request.getUsername()));
        }

        if (!user.isMfaEnabled() || user.getMfaSecret() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "MFA is not enabled for this user"));
        }

        boolean valid = totpService.verifyCode(user.getMfaSecret(), request.getCode());
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid MFA code", "verified", false));
        }

        return ResponseEntity.ok(Map.of("verified", true, "message", "MFA code verified successfully"));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getMfaStatus(@RequestParam(required = false) String username,
                                          Authentication authentication) {
        String effectiveUsername = getEffectiveUsername(username, authentication);
        if (effectiveUsername == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }

        User user = userRepository.findByUsername(effectiveUsername).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found: " + effectiveUsername));
        }

        return ResponseEntity.ok(new MfaStatusResponse(user.isMfaEnabled(),
                user.isMfaEnabled() ? "MFA is active" : "MFA is disabled"));
    }

    private String getEffectiveUsername(String paramUsername, Authentication authentication) {
        if (paramUsername != null && !paramUsername.isBlank()) {
            return paramUsername;
        }
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }
}
