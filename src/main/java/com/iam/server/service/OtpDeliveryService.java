package com.iam.server.service;

import java.time.Duration;

public interface OtpDeliveryService {

    String generateOtp(int length);

    void storeOtp(String key, String otp, Duration ttl);

    boolean verifyStoredOtp(String key, String otp);

    boolean sendEmailOtp(String toEmail, String otp);

    boolean sendSmsOtp(String toPhoneNumber, String otp);
}
