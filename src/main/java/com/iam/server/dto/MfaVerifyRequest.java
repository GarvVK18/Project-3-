package com.iam.server.dto;

public class MfaVerifyRequest {

    private String username;
    private int code;

    public MfaVerifyRequest() {
    }

    public MfaVerifyRequest(String username, int code) {
        this.username = username;
        this.code = code;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
