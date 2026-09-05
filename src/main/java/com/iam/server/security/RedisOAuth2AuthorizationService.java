package com.iam.server.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Service;

@Service
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(RedisOAuth2AuthorizationService.class);

    private static final Duration AUTH_CODE_TTL = Duration.ofMinutes(5);
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(1);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // Local cache backed by distributed Redis indexes and TTLs
    private final Map<String, OAuth2Authorization> authorizations = new ConcurrentHashMap<>();
    private final Map<String, String> tokenToIdIndex = new ConcurrentHashMap<>();

    @Override
    public void save(OAuth2Authorization authorization) {
        if (authorization == null) {
            return;
        }

        authorizations.put(authorization.getId(), authorization);

        OAuth2Authorization.Token<OAuth2AuthorizationCode> authCode =
                authorization.getToken(OAuth2AuthorizationCode.class);
        if (authCode != null && authCode.getToken() != null) {
            String codeValue = authCode.getToken().getTokenValue();
            tokenToIdIndex.put(codeValue, authorization.getId());
            saveToRedis("oauth2:code:" + codeValue, authorization.getId(), AUTH_CODE_TTL);
            log.info("Cached short-lived OAuth2 authorization code in Redis with TTL: {}s", AUTH_CODE_TTL.getSeconds());
        }

        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken =
                authorization.getToken(OAuth2RefreshToken.class);
        if (refreshToken != null && refreshToken.getToken() != null) {
            String refreshValue = refreshToken.getToken().getTokenValue();
            tokenToIdIndex.put(refreshValue, authorization.getId());
            saveToRedis("oauth2:refresh:" + refreshValue, authorization.getId(), REFRESH_TOKEN_TTL);
            log.info("Cached OAuth2 refresh token in Redis with TTL: {}s", REFRESH_TOKEN_TTL.getSeconds());
        }

        OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
                authorization.getToken(OAuth2AccessToken.class);
        if (accessToken != null && accessToken.getToken() != null) {
            String accessValue = accessToken.getToken().getTokenValue();
            tokenToIdIndex.put(accessValue, authorization.getId());
            saveToRedis("oauth2:access:" + accessValue, authorization.getId(), ACCESS_TOKEN_TTL);
        }

        String state = authorization.getAttribute(OAuth2ParameterNames.STATE);
        if (state != null) {
            tokenToIdIndex.put(state, authorization.getId());
            saveToRedis("oauth2:state:" + state, authorization.getId(), AUTH_CODE_TTL);
        }

        saveToRedis("oauth2:id:" + authorization.getId(), authorization.getPrincipalName(), REFRESH_TOKEN_TTL);
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        if (authorization == null) {
            return;
        }

        authorizations.remove(authorization.getId());

        OAuth2Authorization.Token<OAuth2AuthorizationCode> authCode =
                authorization.getToken(OAuth2AuthorizationCode.class);
        if (authCode != null && authCode.getToken() != null) {
            String codeValue = authCode.getToken().getTokenValue();
            tokenToIdIndex.remove(codeValue);
            deleteFromRedis("oauth2:code:" + codeValue);
        }

        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken =
                authorization.getToken(OAuth2RefreshToken.class);
        if (refreshToken != null && refreshToken.getToken() != null) {
            String refreshValue = refreshToken.getToken().getTokenValue();
            tokenToIdIndex.remove(refreshValue);
            deleteFromRedis("oauth2:refresh:" + refreshValue);
        }

        OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
                authorization.getToken(OAuth2AccessToken.class);
        if (accessToken != null && accessToken.getToken() != null) {
            String accessValue = accessToken.getToken().getTokenValue();
            tokenToIdIndex.remove(accessValue);
            deleteFromRedis("oauth2:access:" + accessValue);
        }

        deleteFromRedis("oauth2:id:" + authorization.getId());
    }

    @Nullable
    @Override
    public OAuth2Authorization findById(String id) {
        if (id == null) {
            return null;
        }
        return authorizations.get(id);
    }

    @Nullable
    @Override
    public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType tokenType) {
        if (token == null) {
            return null;
        }

        // Check local token-to-id mapping first
        String authId = tokenToIdIndex.get(token);

        // If not in local memory, lookup in Redis
        if (authId == null) {
            authId = lookupRedisToken(token, tokenType);
        }

        if (authId != null) {
            return authorizations.get(authId);
        }

        // Fallback scan
        for (OAuth2Authorization authorization : authorizations.values()) {
            if (hasToken(authorization, token, tokenType)) {
                return authorization;
            }
        }

        return null;
    }

    private boolean hasToken(OAuth2Authorization authorization, String token, @Nullable OAuth2TokenType tokenType) {
        if (tokenType == null) {
            return (authorization.getToken(token) != null) ||
                    token.equals(authorization.getAttribute(OAuth2ParameterNames.STATE));
        }

        if (OAuth2ParameterNames.STATE.equals(tokenType.getValue())) {
            return token.equals(authorization.getAttribute(OAuth2ParameterNames.STATE));
        }

        OAuth2Authorization.Token<?> authorizationToken = authorization.getToken(token);
        return authorizationToken != null;
    }

    private void saveToRedis(String key, String value, Duration ttl) {
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                redisTemplate.opsForValue().set(key, value, ttl);
            }
        } catch (Exception e) {
            log.warn("Failed to write to Redis (offline/unreachable): {}", e.getMessage());
        }
    }

    private void deleteFromRedis(String key) {
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                redisTemplate.delete(key);
            }
        } catch (Exception ignored) {
        }
    }

    private String lookupRedisToken(String token, @Nullable OAuth2TokenType tokenType) {
        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                if (tokenType != null) {
                    if (OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
                        return redisTemplate.opsForValue().get("oauth2:code:" + token);
                    } else if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
                        return redisTemplate.opsForValue().get("oauth2:refresh:" + token);
                    } else if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
                        return redisTemplate.opsForValue().get("oauth2:access:" + token);
                    } else if (OAuth2ParameterNames.STATE.equals(tokenType.getValue())) {
                        return redisTemplate.opsForValue().get("oauth2:state:" + token);
                    }
                }
                // Try checking code, refresh, access in order
                String id = redisTemplate.opsForValue().get("oauth2:code:" + token);
                if (id != null) return id;
                id = redisTemplate.opsForValue().get("oauth2:refresh:" + token);
                if (id != null) return id;
                return redisTemplate.opsForValue().get("oauth2:access:" + token);
            }
        } catch (Exception e) {
            log.warn("Failed to read token from Redis: {}", e.getMessage());
        }
        return null;
    }
}
