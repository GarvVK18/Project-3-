package com.iam.server.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

class RedisOAuth2AuthorizationServiceTest {

    private RedisOAuth2AuthorizationService authorizationService;
    private RegisteredClient registeredClient;

    @BeforeEach
    void setUp() {
        authorizationService = new RedisOAuth2AuthorizationService();
        registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("test-client")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/callback")
                .build();
    }

    @Test
    void testSaveAndFindById() {
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id("auth-123")
                .principalName("alice")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .build();

        authorizationService.save(authorization);

        OAuth2Authorization found = authorizationService.findById("auth-123");
        assertNotNull(found);
        assertEquals("alice", found.getPrincipalName());
    }

    @Test
    void testSaveAndFindByToken() {
        OAuth2AuthorizationCode code = new OAuth2AuthorizationCode(
                "auth-code-xyz",
                Instant.now(),
                Instant.now().plusSeconds(300)
        );

        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id("auth-456")
                .principalName("bob")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .token(code)
                .build();

        authorizationService.save(authorization);

        OAuth2Authorization found = authorizationService.findByToken("auth-code-xyz", new OAuth2TokenType("code"));
        assertNotNull(found);
        assertEquals("bob", found.getPrincipalName());
    }

    @Test
    void testRemoveAuthorization() {
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .id("auth-789")
                .principalName("charlie")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .build();

        authorizationService.save(authorization);
        assertNotNull(authorizationService.findById("auth-789"));

        authorizationService.remove(authorization);
        assertNull(authorizationService.findById("auth-789"));
    }
}
