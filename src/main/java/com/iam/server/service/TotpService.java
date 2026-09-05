package com.iam.server.service;

public interface TotpService {

    String generateSecretKey();

    String generateQrCodeUri(String username, String secretKey);

    boolean verifyCode(String secretKey, int code);

    int generateCurrentCode(String secretKey);
}
