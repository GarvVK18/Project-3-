package com.iam.server.dto;

public class PasswordResetRequest {

    private String username;

    public PasswordResetRequest() {
    }

    public PasswordResetRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
