package com.iam.server.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iam.server.dto.MfaChallengeResponse;
import com.iam.server.dto.MfaSetupResponse;
import com.iam.server.entity.User;
import com.iam.server.repository.UserRepository;

@Service
public class MfaServiceImpl implements MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaServiceImpl.class);
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final TotpService totpService;
    private final OtpDeliveryService otpDeliveryService;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // In-memory fallback if Redis is unavailable
    private final Map<String, ChallengeEntry> inMemoryChallenges = new ConcurrentHashMap<>();
    private final Map<String, PendingSetup> inMemoryPendingSetups = new ConcurrentHashMap<>();

    private static class ChallengeEntry {
        final String username;
        final Instant expiresAt;

        ChallengeEntry(String username, Instant expiresAt) {
            this.username = username;
            this.expiresAt = expiresAt;
        }
    }

    private static class PendingSetup {
        final String secretKey;
        final String mfaType;
        final Instant expiresAt;

        PendingSetup(String secretKey, String mfaType, Instant expiresAt) {
            this.secretKey = secretKey;
            this.mfaType = mfaType;
            this.expiresAt = expiresAt;
        }
    }

    public MfaServiceImpl(
            UserRepository userRepository,
            TotpService totpService,
            OtpDeliveryService otpDeliveryService) {
        this.userRepository = userRepository;
        this.totpService = totpService;
        this.otpDeliveryService = otpDeliveryService;
    }

    @Override
    public MfaSetupResponse setupMfa(String username, String mfaType) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        String normalizedType = (mfaType == null || mfaType.isBlank()) ? "TOTP" : mfaType.toUpperCase();

        if ("TOTP".equals(normalizedType)) {
            String secretKey = totpService.generateSecretKey();
            String qrUri = totpService.generateQrCodeUri(username, secretKey);

            storePendingSetup(username, secretKey, normalizedType);

            return new MfaSetupResponse(
                    secretKey,
                    qrUri,
                    normalizedType,
                    "Scan the QR code in Google Authenticator or enter the secret key manually, then submit a verification code to activate."
            );
        } else if ("EMAIL".equals(normalizedType) || "SMS".equals(normalizedType)) {
            String otp = otpDeliveryService.generateOtp(6);
            otpDeliveryService.storeOtp("setup:" + username, otp, CHALLENGE_TTL);

            if ("EMAIL".equals(normalizedType)) {
                otpDeliveryService.sendEmailOtp(user.getEmail(), otp);
            } else {
                otpDeliveryService.sendSmsOtp(user.getPhoneNumber(), otp);
            }

            storePendingSetup(username, "", normalizedType);

            return new MfaSetupResponse(
                    null,
                    null,
                    normalizedType,
                    "Verification OTP has been sent to your registered " + normalizedType.toLowerCase() + ". Enter the code to activate MFA."
            );
        } else {
            throw new IllegalArgumentException("Unsupported MFA type: " + mfaType + ". Supported: TOTP, EMAIL, SMS");
        }
    }

    @Override
    @Transactional
    public boolean enableMfa(String username, String code, String mfaType, String email, String phoneNumber) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        String normalizedType = (mfaType == null || mfaType.isBlank()) ? "TOTP" : mfaType.toUpperCase();
        PendingSetup pending = getPendingSetup(username);

        if ("TOTP".equals(normalizedType)) {
            String secretKey = (pending != null && pending.secretKey != null && !pending.secretKey.isBlank())
                    ? pending.secretKey
                    : user.getTotpSecret();

            if (secretKey == null || secretKey.isBlank()) {
                throw new IllegalStateException("No pending TOTP secret found. Please call setup first.");
            }

            try {
                int numericCode = Integer.parseInt(code.trim());
                if (!totpService.verifyCode(secretKey, numericCode)) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }

            user.setTotpSecret(secretKey);
        } else if ("EMAIL".equals(normalizedType) || "SMS".equals(normalizedType)) {
            boolean valid = otpDeliveryService.verifyStoredOtp("setup:" + username, code);
            if (!valid) {
                return false;
            }
        }

        if (email != null && !email.isBlank()) {
            user.setEmail(email.trim());
        }
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            user.setPhoneNumber(phoneNumber.trim());
        }

        user.setMfaEnabled(true);
        user.setMfaType(normalizedType);
        userRepository.save(user);

        clearPendingSetup(username);
        return true;
    }

    @Override
    @Transactional
    public boolean disableMfa(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        user.setMfaEnabled(false);
        user.setMfaType("NONE");
        user.setTotpSecret(null);
        userRepository.save(user);
        return true;
    }

    @Override
    public MfaChallengeResponse initiateLoginChallenge(User user) {
        String tempToken = UUID.randomUUID().toString();
        storeChallenge(tempToken, user.getUsername());

        String mfaType = (user.getMfaType() != null && !user.getMfaType().isBlank()) ? user.getMfaType() : "TOTP";

        if ("EMAIL".equals(mfaType)) {
            String otp = otpDeliveryService.generateOtp(6);
            otpDeliveryService.storeOtp("challenge:" + user.getUsername(), otp, CHALLENGE_TTL);
            otpDeliveryService.sendEmailOtp(user.getEmail(), otp);
        } else if ("SMS".equals(mfaType)) {
            String otp = otpDeliveryService.generateOtp(6);
            otpDeliveryService.storeOtp("challenge:" + user.getUsername(), otp, CHALLENGE_TTL);
            otpDeliveryService.sendSmsOtp(user.getPhoneNumber(), otp);
        }

        return new MfaChallengeResponse(
                true,
                tempToken,
                mfaType,
                "Two-factor authentication challenge required. Please provide your " + mfaType + " code."
        );
    }

    @Override
    public boolean sendOtpForChallenge(String tempToken) {
        String username = getUsernameFromChallenge(tempToken);
        if (username == null) {
            return false;
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        String mfaType = user.getMfaType();
        String otp = otpDeliveryService.generateOtp(6);
        otpDeliveryService.storeOtp("challenge:" + username, otp, CHALLENGE_TTL);

        if ("EMAIL".equals(mfaType)) {
            return otpDeliveryService.sendEmailOtp(user.getEmail(), otp);
        } else if ("SMS".equals(mfaType)) {
            return otpDeliveryService.sendSmsOtp(user.getPhoneNumber(), otp);
        }

        return false;
    }

    @Override
    public String verifyLoginChallenge(String tempToken, String code) {
        String username = getUsernameFromChallenge(tempToken);
        if (username == null) {
            return null;
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !user.isMfaEnabled()) {
            return null;
        }

        String mfaType = user.getMfaType() != null ? user.getMfaType() : "TOTP";

        boolean verified = false;
        if ("TOTP".equals(mfaType)) {
            try {
                int numericCode = Integer.parseInt(code.trim());
                verified = totpService.verifyCode(user.getTotpSecret(), numericCode);
            } catch (NumberFormatException e) {
                verified = false;
            }
        } else if ("EMAIL".equals(mfaType) || "SMS".equals(mfaType)) {
            verified = otpDeliveryService.verifyStoredOtp("challenge:" + username, code);
        }

        if (verified) {
            removeChallenge(tempToken);
            return username;
        }

        return null;
    }

    private void storeChallenge(String tempToken, String username) {
        String redisKey = "mfa:challenge:" + tempToken;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                redisTemplate.opsForValue().set(redisKey, username, CHALLENGE_TTL);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, using in-memory store for MFA challenge: {}", e.getMessage());
        }

        inMemoryChallenges.put(tempToken, new ChallengeEntry(username, Instant.now().plus(CHALLENGE_TTL)));
    }

    private String getUsernameFromChallenge(String tempToken) {
        if (tempToken == null) {
            return null;
        }

        String redisKey = "mfa:challenge:" + tempToken;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                return redisTemplate.opsForValue().get(redisKey);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, reading from in-memory store: {}", e.getMessage());
        }

        ChallengeEntry entry = inMemoryChallenges.get(tempToken);
        if (entry != null && Instant.now().isBefore(entry.expiresAt)) {
            return entry.username;
        }

        return null;
    }

    private void removeChallenge(String tempToken) {
        String redisKey = "mfa:challenge:" + tempToken;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                redisTemplate.delete(redisKey);
            }
        } catch (Exception ignored) {
        }
        inMemoryChallenges.remove(tempToken);
    }

    private void storePendingSetup(String username, String secretKey, String mfaType) {
        String redisKey = "mfa:pending:" + username;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                redisTemplate.opsForValue().set(redisKey, secretKey + ":" + mfaType, Duration.ofMinutes(15));
                return;
            }
        } catch (Exception ignored) {
        }
        inMemoryPendingSetups.put(username, new PendingSetup(secretKey, mfaType, Instant.now().plus(Duration.ofMinutes(15))));
    }

    private PendingSetup getPendingSetup(String username) {
        String redisKey = "mfa:pending:" + username;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                String val = redisTemplate.opsForValue().get(redisKey);
                if (val != null) {
                    String[] parts = val.split(":", 2);
                    return new PendingSetup(parts[0], parts.length > 1 ? parts[1] : "TOTP", Instant.now().plus(Duration.ofMinutes(15)));
                }
            }
        } catch (Exception ignored) {
        }
        return inMemoryPendingSetups.get(username);
    }

    private void clearPendingSetup(String username) {
        String redisKey = "mfa:pending:" + username;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                redisTemplate.delete(redisKey);
            }
        } catch (Exception ignored) {
        }
        inMemoryPendingSetups.remove(username);
    }
}
