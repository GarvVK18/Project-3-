package com.iam.server.dto;

public class MfaChallengeResponse {

    private boolean mfaRequired;
    private String tempToken;
    private String mfaType;
    private String message;

    public MfaChallengeResponse() {
    }

    public MfaChallengeResponse(boolean mfaRequired, String tempToken, String mfaType, String message) {
        this.mfaRequired = mfaRequired;
        this.tempToken = tempToken;
        this.mfaType = mfaType;
        this.message = message;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

    public String getTempToken() {
        return tempToken;
    }

    public void setTempToken(String tempToken) {
        this.tempToken = tempToken;
    }

    public String getMfaType() {
        return mfaType;
    }

    public void setMfaType(String mfaType) {
        this.mfaType = mfaType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
