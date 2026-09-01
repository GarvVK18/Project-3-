package com.iam.server.controller;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iam.server.dto.UpdateProfileRequest;
import com.iam.server.dto.UserProfileResponse;
import com.iam.server.entity.User;
import com.iam.server.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    private final UserService userService;

    public UserProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());

        UserProfileResponse response = new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.isEnabled(),
                roleNames
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest request) {

        String currentUsername = userDetails.getUsername();

        User updated = userService.updateUsername(currentUsername, request.getUsername());

        Set<String> roleNames = updated.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());

        UserProfileResponse response = new UserProfileResponse(
                updated.getId(),
                updated.getUsername(),
                updated.isEnabled(),
                roleNames
        );

        return ResponseEntity.ok(response);
    }
}
