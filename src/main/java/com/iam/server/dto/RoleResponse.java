package com.iam.server.dto;

import java.util.Set;

public class RoleResponse {

    private Long id;
    private String name;
    private Set<String> authorities;

    public RoleResponse() {
    }

    public RoleResponse(Long id, String name, Set<String> authorities) {
        this.id = id;
        this.name = name;
        this.authorities = authorities;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<String> authorities) {
        this.authorities = authorities;
    }
}
