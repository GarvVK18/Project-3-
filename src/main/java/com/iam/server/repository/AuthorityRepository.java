package com.iam.server.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.iam.server.entity.Authority;

public interface AuthorityRepository extends JpaRepository<Authority, Long> {

    Optional<Authority> findByName(String name);

}