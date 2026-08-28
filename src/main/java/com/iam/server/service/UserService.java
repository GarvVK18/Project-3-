package com.iam.server.service;

import java.util.Optional;

import com.iam.server.entity.User;

public interface UserService {

    User saveUser(User user);

    Optional<User> findByUsername(String username);

}