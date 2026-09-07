package com.iam.server.dto;

public class MfaOtpDispatchRequest {
    private String destination; // Phone number or email address
    private String channel;     // "SMS" or "EMAIL"

    public MfaOtpDispatchRequest() {}

    public MfaOtpDispatchRequest(String destination, String channel) {
        this.destination = destination;
        this.channel = channel;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
