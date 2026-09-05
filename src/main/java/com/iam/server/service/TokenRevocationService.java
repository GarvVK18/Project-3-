package com.iam.server.service;

public interface TokenRevocationService {

    void revokeToken(String token);

    void forceLogoutUser(String username);

    boolean isTokenRevoked(String token);
}
