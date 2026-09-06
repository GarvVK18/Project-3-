package com.iam.server.dto;

public class MfaSetupResponse {

    private String secret;
    private String qrUri;
    private String manualKey;

    public MfaSetupResponse() {
    }

    public MfaSetupResponse(String secret, String qrUri, String manualKey) {
        this.secret = secret;
        this.qrUri = qrUri;
        this.manualKey = manualKey;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getQrUri() {
        return qrUri;
    }

    public void setQrUri(String qrUri) {
        this.qrUri = qrUri;
    }

    public String getManualKey() {
        return manualKey;
    }

    public void setManualKey(String manualKey) {
        this.manualKey = manualKey;
    }
}
