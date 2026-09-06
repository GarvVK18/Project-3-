package com.iam.server.security;

import com.iam.server.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuditSecurityEventListenerTest {

    private AuditLogService auditLogService;
    private AuditSecurityEventListener listener;

    @BeforeEach
    void setUp() {
        auditLogService = mock(AuditLogService.class);
        listener = new AuditSecurityEventListener(auditLogService);
    }

    @Test
    void onAuthenticationSuccess_shouldLogSuccess() {
        Authentication auth = new UsernamePasswordAuthenticationToken("pranav", "pass");
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(auth);

        listener.onAuthenticationSuccess(event);

        verify(auditLogService).logEvent(eq("pranav"), eq("LOGIN_SUCCESS"), eq("SUCCESS"), anyString(), anyString());
    }

    @Test
    void onAuthenticationFailure_shouldLogFailure() {
        Authentication auth = new UsernamePasswordAuthenticationToken("pranav", "badpass");
        AuthenticationFailureBadCredentialsEvent event =
                new AuthenticationFailureBadCredentialsEvent(auth, new BadCredentialsException("Invalid password"));

        listener.onAuthenticationFailure(event);

        verify(auditLogService).logEvent(eq("pranav"), eq("LOGIN_FAILURE"), eq("FAILURE"), anyString(), contains("Invalid password"));
    }
}
