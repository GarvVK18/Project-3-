package com.iam.server.dto;

public class MfaLoginVerificationRequest {

    private String tempToken;
    private String code;

    public MfaLoginVerificationRequest() {
    }

    public MfaLoginVerificationRequest(String tempToken, String code) {
        this.tempToken = tempToken;
        this.code = code;
    }

    public String getTempToken() {
        return tempToken;
    }

    public void setTempToken(String tempToken) {
        this.tempToken = tempToken;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
