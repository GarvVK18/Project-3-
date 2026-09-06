package com.iam.server.service;

public interface TokenBlacklistService {

    void blacklistToken(String token, long expirationMillis);

    boolean isTokenBlacklisted(String token);

    void revokeUserTokens(String username);

    boolean isUserRevoked(String username);
}
