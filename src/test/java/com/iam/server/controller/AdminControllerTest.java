package com.iam.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

import com.iam.server.entity.Authority;
import com.iam.server.entity.Role;
import com.iam.server.entity.User;
import com.iam.server.service.RoleService;
import com.iam.server.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private AdminController adminController;

    private Role role;
    private User user;

    @BeforeEach
void setUp() {
    role = new Role("ADMIN");

    user = new User("pranav", "password");
    user.setRoles(Set.of(role));
}
    @Test
    void createRole_shouldReturnRoleResponse() {
        Authority readAuthority = mock(Authority.class);
        when(readAuthority.getName()).thenReturn("READ");
        role.setAuthorities(Set.of(readAuthority));
        when(roleService.createRole("ADMIN")).thenReturn(role);


        ResponseEntity<?> response = adminController.createRole("ADMIN");

        assertEquals(200, response.getStatusCode().value());

        var body = response.getBody();
        assertNotNull(body);
        assertEquals("ADMIN", ((com.iam.server.dto.RoleResponse) body).getName());
        assertEquals(Set.of("READ"),
                ((com.iam.server.dto.RoleResponse) body).getAuthorities());

        verify(roleService).createRole("ADMIN");
    }

    @Test
    void listRoles_shouldReturnAllRoles() {
        Role userRole = new Role("USER");

        when(roleService.findAll()).thenReturn(List.of(role, userRole));

        ResponseEntity<?> response = adminController.listRoles();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        List<?> body = (List<?>) response.getBody();

        assertEquals(2, body.size());
        assertEquals("ADMIN",
                ((com.iam.server.dto.RoleResponse) body.get(0)).getName());
        assertEquals("USER",
                ((com.iam.server.dto.RoleResponse) body.get(1)).getName());

        verify(roleService).findAll();
    }

    @Test
    void assignAuthorityToRole_shouldReturnUpdatedRole() {
        Authority readAuthority = mock(Authority.class);
        when(readAuthority.getName()).thenReturn("READ");
        role.setAuthorities(Set.of(readAuthority));
        when(roleService.assignAuthorityToRole("ADMIN", "READ"))
                .thenReturn(role);

        ResponseEntity<?> response =
                adminController.assignAuthorityToRole("ADMIN", "READ");

        assertEquals(200, response.getStatusCode().value());

        var body = (com.iam.server.dto.RoleResponse) response.getBody();

        assertNotNull(body);
        assertEquals("ADMIN", body.getName());
        assertEquals(Set.of("READ"), body.getAuthorities());

        verify(roleService).assignAuthorityToRole("ADMIN", "READ");
    }

    @Test
    void removeAuthorityFromRole_shouldReturnUpdatedRole() {
        when(roleService.removeAuthorityFromRole("ADMIN", "READ"))
                .thenReturn(role);

        ResponseEntity<?> response =
                adminController.removeAuthorityFromRole("ADMIN", "READ");

        assertEquals(200, response.getStatusCode().value());

        var body = (com.iam.server.dto.RoleResponse) response.getBody();

        assertNotNull(body);
        assertEquals("ADMIN", body.getName());

        verify(roleService).removeAuthorityFromRole("ADMIN", "READ");
    }

    @Test
    void assignRoleToUser_shouldReturnUserProfile() {
        when(userService.assignRoleToUser("pranav", "ADMIN"))
                .thenReturn(user);

        ResponseEntity<?> response =
                adminController.assignRoleToUser("pranav", "ADMIN");

        assertEquals(200, response.getStatusCode().value());

        var body =
                (com.iam.server.dto.UserProfileResponse) response.getBody();

        assertNotNull(body);
        assertEquals("pranav", body.getUsername());
        assertTrue(body.isEnabled());
        assertEquals(Set.of("ADMIN"), body.getRoles());

        verify(userService).assignRoleToUser("pranav", "ADMIN");
    }

    @Test
    void removeRoleFromUser_shouldReturnUserProfile() {
        when(userService.removeRoleFromUser("pranav", "ADMIN"))
                .thenReturn(user);

        ResponseEntity<?> response =
                adminController.removeRoleFromUser("pranav", "ADMIN");

        assertEquals(200, response.getStatusCode().value());

        var body =
                (com.iam.server.dto.UserProfileResponse) response.getBody();

        assertNotNull(body);
        assertEquals("pranav", body.getUsername());
        assertEquals(Set.of("ADMIN"), body.getRoles());

        verify(userService).removeRoleFromUser("pranav", "ADMIN");
    }
}