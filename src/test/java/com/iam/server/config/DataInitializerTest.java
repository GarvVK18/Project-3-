package com.iam.server.config;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.iam.server.entity.Authority;
import com.iam.server.entity.Role;
import com.iam.server.entity.User;
import com.iam.server.repository.AuthorityRepository;
import com.iam.server.repository.RoleRepository;
import com.iam.server.repository.UserRepository;

class DataInitializerTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private AuthorityRepository authorityRepository;
    private PasswordEncoder passwordEncoder;
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        authorityRepository = mock(AuthorityRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        dataInitializer = new DataInitializer(userRepository, roleRepository, authorityRepository, passwordEncoder);
    }

    @Test
    void testRun_seedsDefaultRolesAndAdminWhenEmpty() {
        when(authorityRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(authorityRepository.save(any(Authority.class))).thenAnswer(i -> i.getArgument(0));

        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(i -> i.getArgument(0));

        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");

        dataInitializer.run();

        verify(userRepository).save(any(User.class));
        verify(roleRepository, atLeast(2)).save(any(Role.class));
    }

    @Test
    void testRun_skipsAdminCreationWhenAlreadyExists() {
        when(authorityRepository.findByName(anyString())).thenReturn(Optional.of(new Authority("AUTH")));
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(new Role("ROLE")));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User("admin", "pass")));

        dataInitializer.run();

        verify(userRepository, never()).save(any(User.class));
    }
}
