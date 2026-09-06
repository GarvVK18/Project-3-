package com.iam.server.dto;

public class MfaStatusResponse {

    private boolean mfaEnabled;
    private String message;

    public MfaStatusResponse() {
    }

    public MfaStatusResponse(boolean mfaEnabled, String message) {
        this.mfaEnabled = mfaEnabled;
        this.message = message;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
