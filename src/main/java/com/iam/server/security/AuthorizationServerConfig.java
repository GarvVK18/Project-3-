package com.iam.server.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AuthorizationServerConfig {

    // =====================================================
    // 1. AUTHORIZATION SERVER SECURITY
    // =====================================================

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http) throws Exception {

        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.formLogin(Customizer.withDefaults());

        return http.build();
    }

    // =====================================================
    // 2. REGISTER OAUTH2 CLIENT
    // =====================================================

    @Bean
    public RegisteredClientRepository registeredClientRepository() {

        RegisteredClient client = RegisteredClient
                .withId(UUID.randomUUID().toString())

                // CLIENT ID
                .clientId("project3-client")

                // CLIENT SECRET
                .clientSecret("{noop}project3-secret")

                // Allow Basic authentication
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC)

                // Also allow credentials in request body
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_POST)

                // CLIENT CREDENTIALS
                .authorizationGrantType(
                        AuthorizationGrantType.CLIENT_CREDENTIALS)

                // Authorization Code
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE)

                // Refresh Token
                .authorizationGrantType(
                        AuthorizationGrantType.REFRESH_TOKEN)

                // Redirect URI
                .redirectUri(
                        "http://127.0.0.1:8080/login/oauth2/code/project3")

                // Scopes
                .scope("openid")
                .scope("profile")

                // Client settings
                .clientSettings(
                        ClientSettings.builder()
                                .requireAuthorizationConsent(false)
                                .build())

                .build();

        return new InMemoryRegisteredClientRepository(client);
    }

    // =====================================================
    // 3. RSA KEY FOR JWT
    // =====================================================

    @Bean
    public JWKSource<SecurityContext> jwkSource() {

        KeyPair keyPair = generateRsaKey();

        RSAPublicKey publicKey =
                (RSAPublicKey) keyPair.getPublic();

        RSAPrivateKey privateKey =
                (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);

        return (selector, context) ->
                selector.select(jwkSet);
    }

    // =====================================================
    // 4. GENERATE RSA KEY
    // =====================================================

    private static KeyPair generateRsaKey() {

        try {

            KeyPairGenerator keyPairGenerator =
                    KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);

            return keyPairGenerator.generateKeyPair();

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to generate RSA key",
                    exception);
        }
    }

    // =====================================================
    // 5. AUTHORIZATION SERVER SETTINGS
    // =====================================================

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {

        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:9000")
                .build();
    }
}