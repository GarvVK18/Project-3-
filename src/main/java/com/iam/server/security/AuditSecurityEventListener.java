package com.iam.server.security;

import com.iam.server.service.AuditLogService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

@Component
public class AuditSecurityEventListener {

    private final AuditLogService auditLogService;

    public AuditSecurityEventListener(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        String clientIp = extractClientIp(event.getAuthentication().getDetails());
        auditLogService.logEvent(username, "LOGIN_SUCCESS", "SUCCESS", clientIp, "User logged in successfully");
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication() != null ? event.getAuthentication().getName() : "UNKNOWN";
        String clientIp = event.getAuthentication() != null ? extractClientIp(event.getAuthentication().getDetails()) : "UNKNOWN";
        String failureReason = event.getException() != null ? event.getException().getMessage() : "Bad credentials";
        auditLogService.logEvent(username, "LOGIN_FAILURE", "FAILURE", clientIp, failureReason);
    }

    private String extractClientIp(Object details) {
        if (details instanceof WebAuthenticationDetails webDetails) {
            return webDetails.getRemoteAddress();
        }
        return "127.0.0.1";
    }
}
