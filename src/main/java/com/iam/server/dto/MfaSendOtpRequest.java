package com.iam.server.dto;

public class MfaSendOtpRequest {

    private String tempToken;

    public MfaSendOtpRequest() {
    }

    public MfaSendOtpRequest(String tempToken) {
        this.tempToken = tempToken;
    }

    public String getTempToken() {
        return tempToken;
    }

    public void setTempToken(String tempToken) {
        this.tempToken = tempToken;
    }
}
