package com.iam.server.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iam.server.entity.User;
import com.iam.server.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthController(
            UserService userService,
            AuthenticationManager authenticationManager) {

        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @io.swagger.v3.oas.annotations.Operation(
        summary = "Register new user",
        description = "Registers a new user account with BCrypt password hashing"
    )
    @PostMapping("/register")
    public ResponseEntity<User> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "User registration details",
                required = true,
                content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = User.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "Default Registration",
                        value = "{\n  \"username\": \"pranav_test\",\n  \"password\": \"SecretPassword123!\"\n}"
                    )
                )
            )
            @RequestBody User user) {

        User savedUser = userService.saveUser(user);

        return ResponseEntity.ok(savedUser);
    }

    @io.swagger.v3.oas.annotations.Operation(
        summary = "User login",
        description = "Authenticates user credentials and checks rate limiting"
    )
    @PostMapping("/login")
    public ResponseEntity<String> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "User login credentials",
                required = true,
                content = @io.swagger.v3.oas.annotations.media.Content(
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = User.class),
                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "Default Login",
                        value = "{\n  \"username\": \"pranav_test\",\n  \"password\": \"SecretPassword123!\"\n}"
                    )
                )
            )
            @RequestBody User user) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                user.getUsername(),
                                user.getPassword()
                        )
                );

        if (authentication.isAuthenticated()) {
            return ResponseEntity.ok("Login successful");
        }

        return ResponseEntity.status(401).body("Login failed");
    }
}