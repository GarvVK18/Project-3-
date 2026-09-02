package com.iam.server.service;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import com.iam.server.entity.Authority;
import com.iam.server.entity.Role;
import com.iam.server.repository.AuthorityRepository;
import com.iam.server.repository.RoleRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuthorityRepository authorityRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    void createRole_shouldReturnExistingRoleWhenRoleAlreadyExists() {
        Role existingRole = new Role();
        existingRole.setName("ADMIN");

        when(roleRepository.findByName("ADMIN"))
                .thenReturn(Optional.of(existingRole));

        Role result = roleService.createRole("ADMIN");

        assertSame(existingRole, result);
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void createRole_shouldCreateNewRoleWhenRoleDoesNotExist() {
        Role newRole = new Role();
        newRole.setName("USER");

        when(roleRepository.findByName("USER"))
                .thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class)))
                .thenReturn(newRole);

        Role result = roleService.createRole("USER");

        assertSame(newRole, result);
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void findByName_shouldReturnRoleWhenRoleExists() {
        Role role = new Role();
        role.setName("ADMIN");

        when(roleRepository.findByName("ADMIN"))
                .thenReturn(Optional.of(role));

        Optional<Role> result = roleService.findByName("ADMIN");

        assertTrue(result.isPresent());
        assertSame(role, result.get());
    }

    @Test
    void findAll_shouldReturnAllRoles() {
        Role admin = new Role();
        admin.setName("ADMIN");

        Role user = new Role();
        user.setName("USER");

        when(roleRepository.findAll())
                .thenReturn(Arrays.asList(admin, user));

        var result = roleService.findAll();

        assertEquals(2, result.size());
        assertTrue(result.contains(admin));
        assertTrue(result.contains(user));
    }

    @Test
    void assignAuthorityToRole_shouldAssignExistingAuthority() {
        Role role = new Role();
        role.setName("ADMIN");

        Authority authority = new Authority();
        authority.setName("READ");

        when(roleRepository.findByName("ADMIN"))
                .thenReturn(Optional.of(role));
        when(authorityRepository.findByName("READ"))
                .thenReturn(Optional.of(authority));
        when(roleRepository.save(role))
                .thenReturn(role);

        roleService.assignAuthorityToRole("ADMIN", "READ");

        assertTrue(role.getAuthorities().contains(authority));
        verify(roleRepository).save(role);
    }

    @Test
    void assignAuthorityToRole_shouldCreateAuthorityWhenItDoesNotExist() {
        Role role = new Role();
        role.setName("ADMIN");

        Authority authority = new Authority();
        authority.setName("WRITE");

        when(roleRepository.findByName("ADMIN"))
                .thenReturn(Optional.of(role));
        when(authorityRepository.findByName("WRITE"))
                .thenReturn(Optional.empty());
        when(authorityRepository.save(any(Authority.class)))
                .thenReturn(authority);
        when(roleRepository.save(role))
                .thenReturn(role);

        roleService.assignAuthorityToRole("ADMIN", "WRITE");

        assertTrue(role.getAuthorities().contains(authority));
        verify(authorityRepository).save(any(Authority.class));
        verify(roleRepository).save(role);
    }

    @Test
    void assignAuthorityToRole_shouldThrowExceptionWhenRoleDoesNotExist() {
        when(roleRepository.findByName("ADMIN"))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> roleService.assignAuthorityToRole("ADMIN", "READ")
        );

        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void removeAuthorityFromRole_shouldRemoveAuthority() {
        Role role = new Role();
        role.setName("ADMIN");

        Authority authority = new Authority();
        authority.setName("READ");

        role.getAuthorities().add(authority);

        when(roleRepository.findByName("ADMIN"))
                .thenReturn(Optional.of(role));
        when(authorityRepository.findByName("READ"))
                .thenReturn(Optional.of(authority));
        when(roleRepository.save(role))
                .thenReturn(role);

        roleService.removeAuthorityFromRole("ADMIN", "READ");

        assertFalse(role.getAuthorities().contains(authority));
        verify(roleRepository).save(role);
    }

    @Test
    void removeAuthorityFromRole_shouldThrowExceptionWhenRoleDoesNotExist() {
        when(roleRepository.findByName("ADMIN"))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> roleService.removeAuthorityFromRole("ADMIN", "READ")
        );

        verify(roleRepository, never()).save(any(Role.class));
 }
 @Test
void createRole_shouldReturnExistingRole() {
    Role existingRole = new Role("ADMIN");

    when(roleRepository.findByName("ADMIN"))
            .thenReturn(Optional.of(existingRole));

    Role result = roleService.createRole("ADMIN");

    assertEquals(existingRole, result);
    verify(roleRepository, never()).save(any(Role.class));
}
@Test
void removeAuthorityFromRole_shouldThrowWhenAuthorityDoesNotExist() {
    Role role = new Role("ADMIN");

    when(roleRepository.findByName("ADMIN"))
            .thenReturn(Optional.of(role));
    when(authorityRepository.findByName("READ"))
            .thenReturn(Optional.empty());

    assertThrows(
            RuntimeException.class,
            () -> roleService.removeAuthorityFromRole("ADMIN", "READ")
    );

    verify(roleRepository, never()).save(any(Role.class));
}
}