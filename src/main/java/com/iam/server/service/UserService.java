package com.iam.server.service;

import java.util.Optional;

import com.iam.server.entity.Role;
import com.iam.server.entity.User;

public interface UserService {

    User saveUser(User user);

    Optional<User> findByUsername(String username);

    User updateUsername(String currentUsername, String newUsername);

    User assignRoleToUser(String username, String roleName);

    User removeRoleFromUser(String username, String roleName);

    String createPasswordResetToken(String username);

    void resetPassword(String token, String newPassword);

}