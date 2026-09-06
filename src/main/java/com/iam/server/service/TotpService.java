package com.iam.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Service
public class TotpService {

    private static final Logger logger = LoggerFactory.getLogger(TotpService.class);
    private static final String HMAC_ALGO = "HmacSHA1";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int MODULUS = 1_000_000;
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate a 16-byte cryptographically secure random Base32 secret key.
     */
    public String generateSecretKey() {
        byte[] buffer = new byte[16];
        secureRandom.nextBytes(buffer);
        return encodeBase32(buffer);
    }

    /**
     * Generate an otpauth:// URI suitable for QR Code rendering in Google Authenticator / Authy.
     */
    public String getQrCodeUri(String username, String secretKey, String issuer) {
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                encodedIssuer, encodedUsername, secretKey, encodedIssuer);
    }

    /**
     * Generate current TOTP code for a given secret key and epoch seconds.
     */
    public int generateCode(String secretKey, long epochSeconds) {
        long timeStep = epochSeconds / TIME_STEP_SECONDS;
        byte[] keyBytes = decodeBase32(secretKey);

        byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(keyBytes, HMAC_ALGO));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            return binary % MODULUS;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate TOTP", e);
        }
    }

    /**
     * Verify a 6-digit TOTP code, tolerating +/- 1 window (90s tolerance window).
     */
    public boolean verifyCode(String secretKey, int code) {
        long currentEpoch = System.currentTimeMillis() / 1000;
        for (int window = -1; window <= 1; window++) {
            long epoch = currentEpoch + (window * TIME_STEP_SECONDS);
            if (generateCode(secretKey, epoch) == code) {
                return true;
            }
        }
        return false;
    }

    /**
     * Simulates SMS / Email delivery of OTP for two-step verification (Zaalima Week 3 Requirement).
     */
    public void sendSmsEmailOtp(String destination, String otpCode) {
        logger.info("[MFA DISPATCH] Sent OTP [{}] to destination [{}] via SMS/Email gateway.", otpCode, destination);
    }

    // Base32 RFC 4648 encoding helper
    private String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                bitsLeft -= 5;
                result.append(BASE32_CHARS.charAt((buffer >> bitsLeft) & 0x1F));
            }
        }
        if (bitsLeft > 0) {
            result.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return result.toString();
    }

    // Base32 RFC 4648 decoding helper
    private byte[] decodeBase32(String base32) {
        String clean = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        ByteBuffer bytes = ByteBuffer.allocate((clean.length() * 5) / 8);
        int buffer = 0;
        int bitsLeft = 0;
        for (char c : clean.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                bytes.put((byte) ((buffer >> bitsLeft) & 0xFF));
            }
        }
        return bytes.array();
    }
}
