package com.iam.server.controller;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iam.server.dto.MfaChallengeResponse;
import com.iam.server.entity.User;
import com.iam.server.service.MfaService;
import com.iam.server.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @Autowired(required = false)
    private MfaService mfaService;

    public AuthController(
            UserService userService,
            AuthenticationManager authenticationManager) {

        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    public AuthController(
            UserService userService,
            AuthenticationManager authenticationManager,
            MfaService mfaService) {

        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.mfaService = mfaService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user) {

        User savedUser = userService.saveUser(user);

        return ResponseEntity.ok(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                user.getUsername(),
                                user.getPassword()
                        )
                );

        if (authentication.isAuthenticated()) {
            if (mfaService != null) {
                Optional<User> optionalUser = userService.findByUsername(user.getUsername());
                if (optionalUser.isPresent() && optionalUser.get().isMfaEnabled()) {
                    MfaChallengeResponse challenge = mfaService.initiateLoginChallenge(optionalUser.get());
                    String challengeJson = String.format(
                            "{\"mfaRequired\":true,\"tempToken\":\"%s\",\"mfaType\":\"%s\",\"message\":\"%s\"}",
                            challenge.getTempToken(), challenge.getMfaType(), challenge.getMessage()
                    );
                    return ResponseEntity.status(HttpStatus.ACCEPTED).body(challengeJson);
                }
            }
            return ResponseEntity.ok("Login successful");
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login failed");
    }
}