package com.iam.server.service;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class TotpServiceImpl implements TotpService {

    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int TIME_STEP_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int MODULO = 1_000_000;
    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateSecretKey() {
        byte[] buffer = new byte[20]; // 160 bits recommended for TOTP
        secureRandom.nextBytes(buffer);
        return encodeBase32(buffer);
    }

    @Override
    public String generateQrCodeUri(String username, String secretKey) {
        String issuer = "IAM-Server";
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String encodedAccount = URLEncoder.encode(issuer + ":" + username, StandardCharsets.UTF_8);

        return String.format(
                "otpauth://totp/%s?secret=%s&issuer=%s&digits=%d&period=%d",
                encodedAccount, secretKey, encodedIssuer, DIGITS, TIME_STEP_SECONDS
        );
    }

    @Override
    public boolean verifyCode(String secretKey, int code) {
        if (secretKey == null || secretKey.isBlank()) {
            return false;
        }

        long currentInterval = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;

        // Allow a window of ±1 interval (30 seconds before and after) to compensate for clock skew
        for (int i = -1; i <= 1; i++) {
            int calculatedCode = calculateCode(secretKey, currentInterval + i);
            if (calculatedCode == code) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int generateCurrentCode(String secretKey) {
        long currentInterval = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        return calculateCode(secretKey, currentInterval);
    }

    private int calculateCode(String secretKey, long interval) {
        byte[] keyBytes = decodeBase32(secretKey);
        byte[] data = ByteBuffer.allocate(8).putLong(interval).array();

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(keyBytes, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int truncatedHash = ((hash[offset] & 0x7F) << 24) |
                                ((hash[offset + 1] & 0xFF) << 16) |
                                ((hash[offset + 2] & 0xFF) << 8) |
                                (hash[offset + 3] & 0xFF);

            return truncatedHash % MODULO;
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate TOTP code", e);
        }
    }

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

    private byte[] decodeBase32(String base32) {
        String cleaned = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int numBytes = cleaned.length() * 5 / 8;
        byte[] result = new byte[numBytes];

        int buffer = 0;
        int bitsLeft = 0;
        int byteIndex = 0;

        for (char c : cleaned.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) {
                continue;
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bitsLeft -= 8;
                if (byteIndex < numBytes) {
                    result[byteIndex++] = (byte) ((buffer >> bitsLeft) & 0xFF);
                }
            }
        }

        return result;
    }
}
