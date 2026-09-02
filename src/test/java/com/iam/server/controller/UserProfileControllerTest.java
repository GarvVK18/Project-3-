package com.iam.server.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import com.iam.server.dto.UpdateProfileRequest;
import com.iam.server.dto.UserProfileResponse;
import com.iam.server.entity.Role;
import com.iam.server.entity.User;
import com.iam.server.service.UserService;

class UserProfileControllerTest {

    private UserService userService;
    private UserProfileController userProfileController;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userProfileController = new UserProfileController(userService);
        userDetails = mock(UserDetails.class);

        when(userDetails.getUsername()).thenReturn("pranav");
    }

    @Test
    void getProfile_shouldReturnUserProfile() {
        User user = new User("pranav", "password");
        user.setRoles(Set.of(new Role("USER")));

        when(userService.findByUsername("pranav"))
                .thenReturn(java.util.Optional.of(user));

        ResponseEntity<UserProfileResponse> response =
                userProfileController.getProfile(userDetails);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("pranav", response.getBody().getUsername());
        assertTrue(response.getBody().isEnabled());
        assertEquals(Set.of("USER"), response.getBody().getRoles());
    }

    @Test
    void updateProfile_shouldReturnUpdatedUserProfile() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("newpranav");

        User updatedUser = new User("newpranav", "password");
        updatedUser.setRoles(Set.of(new Role("USER")));

        when(userService.updateUsername("pranav", "newpranav"))
                .thenReturn(updatedUser);

        ResponseEntity<UserProfileResponse> response =
                userProfileController.updateProfile(userDetails, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("newpranav", response.getBody().getUsername());
        assertTrue(response.getBody().isEnabled());
        assertEquals(Set.of("USER"), response.getBody().getRoles());
    }
}