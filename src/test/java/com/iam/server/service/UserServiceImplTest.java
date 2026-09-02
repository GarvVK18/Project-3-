package com.iam.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.iam.server.entity.Role;
import com.iam.server.entity.User;
import com.iam.server.repository.RoleRepository;
import com.iam.server.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("pranav", "password123");
    }

    @Test
    void saveUser_shouldEncodePasswordBeforeSaving() {
        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.saveUser(user);

        assertEquals("encodedPassword", savedUser.getPassword());

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(user);
    }

    @Test
    void findByUsername_shouldReturnUserWhenUserExists() {
        when(userRepository.findByUsername("pranav"))
                .thenReturn(Optional.of(user));

        Optional<User> result = userService.findByUsername("pranav");

        assertTrue(result.isPresent());
        assertEquals("pranav", result.get().getUsername());

        verify(userRepository).findByUsername("pranav");
    }

    @Test
    void updateUsername_shouldUpdateUsernameWhenNewUsernameIsAvailable() {
        when(userRepository.findByUsername("pranav"))
                .thenReturn(Optional.of(user));

        when(userRepository.findByUsername("newpranav"))
                .thenReturn(Optional.empty());

        when(userRepository.save(user))
                .thenReturn(user);

        User result = userService.updateUsername("pranav", "newpranav");

        assertEquals("newpranav", result.getUsername());

        verify(userRepository).save(user);
    }

    @Test
    void updateUsername_shouldRejectDuplicateUsername() {
        User anotherUser = new User("newpranav", "password");

        when(userRepository.findByUsername("pranav"))
                .thenReturn(Optional.of(user));

        when(userRepository.findByUsername("newpranav"))
                .thenReturn(Optional.of(anotherUser));

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUsername("pranav", "newpranav")
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void assignRoleToUser_shouldAssignExistingRole() {
        Role role = new Role("ADMIN");

        when(userRepository.findByUsername("pranav"))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByName("ADMIN"))
                .thenReturn(Optional.of(role));

        when(userRepository.save(user))
                .thenReturn(user);

        User result = userService.assignRoleToUser("pranav", "ADMIN");

        assertTrue(result.getRoles().contains(role));

        verify(userRepository).save(user);
    }


    @Test
    void createPasswordResetToken_shouldCreateAndStoreToken() {
        when(userRepository.findByUsername("pranav"))
                .thenReturn(Optional.of(user));

        when(userRepository.save(user))
                .thenReturn(user);

        String token = userService.createPasswordResetToken("pranav");

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(token, user.getPasswordResetToken());
        assertNotNull(user.getPasswordResetTokenExpiry());
        assertTrue(
                user.getPasswordResetTokenExpiry()
                        .isAfter(LocalDateTime.now())
        );

        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_shouldRejectEmptyToken() {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.resetPassword("", "newPassword")
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void resetPassword_shouldRejectInvalidToken() {
        when(userRepository.findByPasswordResetToken("invalid-token"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.resetPassword(
                        "invalid-token",
                        "newPassword"
                )
        );
    }

    @Test
    void resetPassword_shouldRejectExpiredToken() {
        user.setPasswordResetToken("expired-token");
        user.setPasswordResetTokenExpiry(
                LocalDateTime.now().minusMinutes(1)
        );

        when(userRepository.findByPasswordResetToken("expired-token"))
                .thenReturn(Optional.of(user));

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.resetPassword(
                        "expired-token",
                        "newPassword"
                )
        );
    }

    @Test
    void resetPassword_shouldEncodeNewPasswordAndClearResetToken() {
        user.setPasswordResetToken("valid-token");
        user.setPasswordResetTokenExpiry(
                LocalDateTime.now().plusMinutes(30)
        );

        when(userRepository.findByPasswordResetToken("valid-token"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.encode("newPassword"))
                .thenReturn("encodedNewPassword");

        when(userRepository.save(user))
                .thenReturn(user);

        userService.resetPassword("valid-token", "newPassword");

        assertEquals("encodedNewPassword", user.getPassword());
        assertNull(user.getPasswordResetToken());
        assertNull(user.getPasswordResetTokenExpiry());

        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(user);
    }
    @Test
void assignRoleToUser_shouldCreateRoleWhenRoleDoesNotExist() {
    User user = new User("pranav", "password");

    when(userRepository.findByUsername("pranav"))
            .thenReturn(Optional.of(user));
    when(roleRepository.findByName("ADMIN"))
            .thenReturn(Optional.empty());

    Role newRole = new Role("ADMIN");
    when(roleRepository.save(any(Role.class)))
            .thenReturn(newRole);
    when(userRepository.save(user))
            .thenReturn(user);

    User result = userService.assignRoleToUser("pranav", "ADMIN");

    assertEquals(user, result);
    assertTrue(user.getRoles().contains(newRole));

    verify(roleRepository).save(any(Role.class));
    verify(userRepository).save(user);
}
@Test
void removeRoleFromUser_shouldThrowWhenRoleDoesNotExist() {
    User user = new User("pranav", "password");

    when(userRepository.findByUsername("pranav"))
            .thenReturn(Optional.of(user));
    when(roleRepository.findByName("ADMIN"))
            .thenReturn(Optional.empty());

    assertThrows(
            RuntimeException.class,
            () -> userService.removeRoleFromUser("pranav", "ADMIN")
    );

    verify(userRepository, never()).save(any(User.class));
}
@Test
void assignRoleToUser_shouldThrowWhenUserDoesNotExist() {
    when(userRepository.findByUsername("unknown"))
            .thenReturn(Optional.empty());

    assertThrows(
            RuntimeException.class,
            () -> userService.assignRoleToUser("unknown", "ADMIN")
    );

    verify(userRepository, never()).save(any(User.class));
}
@Test
void removeRoleFromUser_shouldThrowWhenUserDoesNotExist() {
    when(userRepository.findByUsername("unknown"))
            .thenReturn(Optional.empty());

    assertThrows(
            RuntimeException.class,
            () -> userService.removeRoleFromUser("unknown", "ADMIN")
    );

    verify(userRepository, never()).save(any(User.class));
}
}