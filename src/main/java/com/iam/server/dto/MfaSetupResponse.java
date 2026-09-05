package com.iam.server.dto;

public class MfaSetupResponse {

    private String secretKey;
    private String qrCodeUri;
    private String mfaType;
    private String message;

    public MfaSetupResponse() {
    }

    public MfaSetupResponse(String secretKey, String qrCodeUri, String mfaType, String message) {
        this.secretKey = secretKey;
        this.qrCodeUri = qrCodeUri;
        this.mfaType = mfaType;
        this.message = message;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getQrCodeUri() {
        return qrCodeUri;
    }

    public void setQrCodeUri(String qrCodeUri) {
        this.qrCodeUri = qrCodeUri;
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
