package com.iam.server.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

import com.iam.server.entity.User;
import com.iam.server.service.UserService;

class AuthControllerTest {

    private UserService userService;
    private AuthenticationManager authenticationManager;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        authenticationManager = mock(AuthenticationManager.class);
        authController = new AuthController(userService, authenticationManager);
    }

    @Test
    void register_shouldReturnSavedUser() {
        User user = new User("pranav", "password");

        when(userService.saveUser(user)).thenReturn(user);

        ResponseEntity<User> response = authController.register(user);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("pranav", response.getBody().getUsername());
    }

    @Test
    void login_shouldReturnSuccessWhenAuthenticated() {
        User user = new User("pranav", "password");

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authenticationManager.authenticate(
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(authentication);

        ResponseEntity<String> response = authController.login(user);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Login successful", response.getBody());
    }

    @Test
    void login_shouldReturnUnauthorizedWhenNotAuthenticated() {
        User user = new User("pranav", "wrong");

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);
        when(authenticationManager.authenticate(
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(authentication);

        ResponseEntity<String> response = authController.login(user);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Login failed", response.getBody());
    }
}