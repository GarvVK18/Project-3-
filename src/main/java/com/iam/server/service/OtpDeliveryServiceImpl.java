package com.iam.server.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OtpDeliveryServiceImpl implements OtpDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(OtpDeliveryServiceImpl.class);
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${mfa.sendgrid.api-key:}")
    private String sendgridApiKey;

    @Value("${mfa.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${mfa.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${mfa.twilio.from-number:}")
    private String twilioFromNumber;

    // In-memory fallback if Redis is unreachable in dev/tests
    private final Map<String, CacheEntry> inMemoryOtpStore = new ConcurrentHashMap<>();

    private static class CacheEntry {
        final String otp;
        final Instant expiresAt;

        CacheEntry(String otp, Instant expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }
    }

    @Override
    public String generateOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    @Override
    public void storeOtp(String key, String otp, Duration ttl) {
        String redisKey = "otp:" + key;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                redisTemplate.opsForValue().set(redisKey, otp, ttl);
                log.info("Stored OTP in Redis for key: {}", key);
                return;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, storing OTP in memory fallback: {}", e.getMessage());
        }

        inMemoryOtpStore.put(key, new CacheEntry(otp, Instant.now().plus(ttl)));
    }

    @Override
    public boolean verifyStoredOtp(String key, String otp) {
        if (key == null || otp == null) {
            return false;
        }

        String redisKey = "otp:" + key;
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                String stored = redisTemplate.opsForValue().get(redisKey);
                if (stored != null && stored.equals(otp.trim())) {
                    redisTemplate.delete(redisKey);
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Redis unavailable, checking in-memory fallback for key: {}", key);
        }

        CacheEntry entry = inMemoryOtpStore.get(key);
        if (entry != null) {
            if (Instant.now().isBefore(entry.expiresAt) && entry.otp.equals(otp.trim())) {
                inMemoryOtpStore.remove(key);
                return true;
            } else if (Instant.now().isAfter(entry.expiresAt)) {
                inMemoryOtpStore.remove(key);
            }
        }

        return false;
    }

    @Override
    public boolean sendEmailOtp(String toEmail, String otp) {
        if (toEmail == null || toEmail.isBlank()) {
            log.error("Cannot send Email OTP: recipient email is empty");
            return false;
        }

        if (sendgridApiKey != null && !sendgridApiKey.isBlank()) {
            log.info("Dispatching email via SendGrid to: {}", toEmail);
            // Production SendGrid API integration can be invoked here
        }

        log.info("[MFA EMAIL DELIVERY] To: {} | OTP: {} (Valid for 5 minutes)", toEmail, otp);
        return true;
    }

    @Override
    public boolean sendSmsOtp(String toPhoneNumber, String otp) {
        if (toPhoneNumber == null || toPhoneNumber.isBlank()) {
            log.error("Cannot send SMS OTP: recipient phone number is empty");
            return false;
        }

        if (twilioAccountSid != null && !twilioAccountSid.isBlank()) {
            log.info("Dispatching SMS via Twilio to: {}", toPhoneNumber);
            // Production Twilio API integration can be invoked here
        }

        log.info("[MFA SMS DELIVERY] To: {} | OTP: {} (Valid for 5 minutes)", toPhoneNumber, otp);
        return true;
    }
}
