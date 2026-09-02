package com.iam.server.security;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;

import java.util.Optional;

import com.iam.server.entity.Authority;
import com.iam.server.entity.Role;
import com.iam.server.entity.User;
import com.iam.server.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import static org.mockito.Mockito.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_shouldReturnUserDetailsWhenUserExists() {
        User user = new User("pranav", "encodedPassword");

        when(userRepository.findByUsername("pranav"))
                .thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("pranav");

        assertNotNull(result);
        assertEquals("pranav", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());

        verify(userRepository).findByUsername("pranav");
    }

    @Test
    void loadUserByUsername_shouldThrowExceptionWhenUserDoesNotExist() {
        when(userRepository.findByUsername("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown")
        );

        verify(userRepository).findByUsername("unknown");
    }

    @Test
    void loadUserByUsername_shouldAddRoleAsGrantedAuthority() {
        User user = new User("pranav", "encodedPassword");

        Role adminRole = new Role("ADMIN");
        user.getRoles().add(adminRole);

        when(userRepository.findByUsername("pranav"))
                .thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("pranav");

        assertTrue(result.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_shouldAddRolePermissionsAsGrantedAuthorities() {
        User user = new User("pranav", "encodedPassword");

        Role adminRole = new Role("ADMIN");

        Authority readAuthority = mock(Authority.class);
        Authority writeAuthority = mock(Authority.class);

        when(readAuthority.getName()).thenReturn("READ");
        when(writeAuthority.getName()).thenReturn("WRITE");

        adminRole.setAuthorities(Set.of(readAuthority, writeAuthority)); 

        user.getRoles().add(adminRole);

        when(userRepository.findByUsername("pranav"))
                .thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("pranav");
        System.out.println("AUTHORITIES = " + result.getAuthorities());

        assertTrue(result.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("READ")));

        assertTrue(result.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("WRITE")));
    }

    @Test
    void loadUserByUsername_shouldAddAuthoritiesFromMultipleRoles() {
        User user = new User("pranav", "encodedPassword");

        Role adminRole = mock(Role.class);
        Role userRole = mock(Role.class);

        when(adminRole.getName()).thenReturn("ADMIN");
        when(userRole.getName()).thenReturn("USER");

        Authority readAuthority = new Authority("READ");
        Authority profileAuthority = new Authority("PROFILE");

        when(adminRole.getAuthorities()).thenReturn(Set.of(readAuthority));
        when(userRole.getAuthorities()).thenReturn(Set.of(profileAuthority));

        user.getRoles().add(adminRole);
        user.getRoles().add(userRole);

        when(userRepository.findByUsername("pranav"))
                .thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("pranav");

        assertTrue(result.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        assertTrue(result.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

        assertTrue(result.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("READ")));

        assertTrue(result.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("PROFILE")));
    }

    @Test
    void loadUserByUsername_shouldReturnDisabledUserWhenUserIsDisabled() {
        User user = new User("pranav", "encodedPassword");
        user.setEnabled(false);

        when(userRepository.findByUsername("pranav"))
                .thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("pranav");

        assertFalse(result.isEnabled());
    }

    @Test
    void loadUserByUsername_shouldReturnEnabledUserWhenUserIsEnabled() {
        User user = new User("pranav", "encodedPassword");
        user.setEnabled(true);

        when(userRepository.findByUsername("pranav"))
                .thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("pranav");

        assertTrue(result.isEnabled());
    }
}