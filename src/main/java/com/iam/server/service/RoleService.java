package com.iam.server.service;

import java.util.List;
import java.util.Optional;

import com.iam.server.entity.Role;

public interface RoleService {

    Role createRole(String name);

    Optional<Role> findByName(String name);

    List<Role> findAll();

    Role assignAuthorityToRole(String roleName, String authorityName);

    Role removeAuthorityFromRole(String roleName, String authorityName);

}
