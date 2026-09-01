package com.iam.server.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iam.server.entity.Authority;
import com.iam.server.entity.Role;
import com.iam.server.repository.AuthorityRepository;
import com.iam.server.repository.RoleRepository;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final AuthorityRepository authorityRepository;

    public RoleServiceImpl(
            RoleRepository roleRepository,
            AuthorityRepository authorityRepository) {

        this.roleRepository = roleRepository;
        this.authorityRepository = authorityRepository;
    }

    @Override
    public Role createRole(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    @Override
    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }

    @Override
    public List<Role> findAll() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional
    public Role assignAuthorityToRole(String roleName, String authorityName) {

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        Authority authority = authorityRepository.findByName(authorityName)
                .orElseGet(() -> authorityRepository.save(new Authority(authorityName)));

        role.getAuthorities().add(authority);

        return roleRepository.save(role);
    }

    @Override
    @Transactional
    public Role removeAuthorityFromRole(String roleName, String authorityName) {

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        Authority authority = authorityRepository.findByName(authorityName)
                .orElseThrow(() -> new RuntimeException("Authority not found: " + authorityName));

        role.getAuthorities().remove(authority);

        return roleRepository.save(role);
    }
}
