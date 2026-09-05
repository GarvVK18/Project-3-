package com.iam.server.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.iam.server.service.AuditLogService;

@Component
public class SecurityAuditEventListener {

    private final AuditLogService auditLogService;

    public SecurityAuditEventListener(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = authentication.getName();
        auditLogService.logEvent(
                "LOGIN_SUCCESS",
                username,
                null,
                "SUCCESS",
                "User authenticated successfully"
        );
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = (authentication != null) ? authentication.getName() : "UNKNOWN";
        String exceptionMessage = (event.getException() != null) ? event.getException().getMessage() : "Bad credentials";

        auditLogService.logEvent(
                "LOGIN_FAILURE",
                username,
                null,
                "FAILURE",
                exceptionMessage
        );
    }
}
