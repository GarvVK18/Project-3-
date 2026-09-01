package com.iam.server.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iam.server.dto.RoleResponse;
import com.iam.server.dto.UserProfileResponse;
import com.iam.server.entity.Role;
import com.iam.server.entity.User;
import com.iam.server.service.RoleService;
import com.iam.server.service.UserService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final RoleService roleService;

    public AdminController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    // --- Role management ---

    @PostMapping("/roles/{roleName}")
    public ResponseEntity<RoleResponse> createRole(@PathVariable String roleName) {
        Role role = roleService.createRole(roleName);
        return ResponseEntity.ok(toRoleResponse(role));
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> listRoles() {
        List<RoleResponse> roles = roleService.findAll().stream()
                .map(this::toRoleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roles);
    }

    @PostMapping("/roles/{roleName}/authorities/{authorityName}")
    public ResponseEntity<RoleResponse> assignAuthorityToRole(
            @PathVariable String roleName,
            @PathVariable String authorityName) {

        Role role = roleService.assignAuthorityToRole(roleName, authorityName);
        return ResponseEntity.ok(toRoleResponse(role));
    }

    @DeleteMapping("/roles/{roleName}/authorities/{authorityName}")
    public ResponseEntity<RoleResponse> removeAuthorityFromRole(
            @PathVariable String roleName,
            @PathVariable String authorityName) {

        Role role = roleService.removeAuthorityFromRole(roleName, authorityName);
        return ResponseEntity.ok(toRoleResponse(role));
    }

    // --- User role assignment ---

    @PostMapping("/users/{username}/roles/{roleName}")
    public ResponseEntity<UserProfileResponse> assignRoleToUser(
            @PathVariable String username,
            @PathVariable String roleName) {

        User user = userService.assignRoleToUser(username, roleName);
        return ResponseEntity.ok(toProfileResponse(user));
    }

    @DeleteMapping("/users/{username}/roles/{roleName}")
    public ResponseEntity<UserProfileResponse> removeRoleFromUser(
            @PathVariable String username,
            @PathVariable String roleName) {

        User user = userService.removeRoleFromUser(username, roleName);
        return ResponseEntity.ok(toProfileResponse(user));
    }

    // --- Helpers ---

    private RoleResponse toRoleResponse(Role role) {
        Set<String> authorityNames = role.getAuthorities().stream()
                .map(a -> a.getName())
                .collect(Collectors.toSet());
        return new RoleResponse(role.getId(), role.getName(), authorityNames);
    }

    private UserProfileResponse toProfileResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet());
        return new UserProfileResponse(user.getId(), user.getUsername(), user.isEnabled(), roleNames);
    }
}
