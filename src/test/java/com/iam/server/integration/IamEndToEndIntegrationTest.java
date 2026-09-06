package com.iam.server.integration;

import com.iam.server.controller.AuditLogController;
import com.iam.server.controller.MfaController;
import com.iam.server.controller.TokenRevocationController;
import com.iam.server.dto.MfaSetupResponse;
import com.iam.server.dto.MfaStatusResponse;
import com.iam.server.dto.MfaVerifyRequest;
import com.iam.server.entity.AuditLog;
import com.iam.server.entity.User;
import com.iam.server.repository.AuditLogRepository;
import com.iam.server.repository.UserRepository;
import com.iam.server.security.RateLimitingFilter;
import com.iam.server.service.AuditLogService;
import com.iam.server.service.TokenBlacklistService;
import com.iam.server.service.TotpService;
import com.iam.server.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class IamEndToEndIntegrationTest {

    private UserRepository userRepository;
    private AuditLogRepository auditLogRepository;
    private AuditLogService auditLogService;
    private TotpService totpService;
    private TokenBlacklistService tokenBlacklistService;
    private RateLimitingFilter rateLimitingFilter;
    private AuditLogController auditLogController;
    private MfaController mfaController;
    private TokenRevocationController tokenRevocationController;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        auditLogRepository = mock(AuditLogRepository.class);
        auditLogService = new AuditLogService(auditLogRepository);
        totpService = new TotpService();
        tokenBlacklistService = mock(TokenBlacklistService.class);
        rateLimitingFilter = new RateLimitingFilter(null);
        auditLogController = new AuditLogController(auditLogService);
        mfaController = new MfaController(userRepository, totpService);
        tokenRevocationController = new TokenRevocationController(tokenBlacklistService);
    }

    @Test
    @DisplayName("End-to-End: Registration, MFA Setup, Verification, Audit Trails, and Token Revocation")
    void testCompleteSecurityAndAuthLifecycle() {
        // Step 1: User Account Registration & Persistence
        User user = new User("pranav", "encryptedSecretPassword");
        when(userRepository.findByUsername("pranav")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Step 2: MFA Setup (TOTP secret & QR Code generation)
        ResponseEntity<?> setupResp = mfaController.setupMfa("pranav", null);
        assertEquals(200, setupResp.getStatusCode().value());
        assertTrue(setupResp.getBody() instanceof MfaSetupResponse);
        MfaSetupResponse setupData = (MfaSetupResponse) setupResp.getBody();
        assertNotNull(setupData.getSecret());
        assertTrue(setupData.getQrUri().startsWith("otpauth://totp/"));

        // Step 3: MFA Verification & Activation
        int currentCode = totpService.generateCode(setupData.getSecret(), System.currentTimeMillis() / 1000);
        ResponseEntity<?> enableResp = mfaController.enableMfa(new MfaVerifyRequest("pranav", currentCode), null);
        assertEquals(200, enableResp.getStatusCode().value());
        assertTrue(((MfaStatusResponse) enableResp.getBody()).isMfaEnabled());
        assertTrue(user.isMfaEnabled());

        // Step 4: Audit Event Recording (Login Success & Token Generation)
        AuditLog successLog = new AuditLog("pranav", "LOGIN_SUCCESS", "SUCCESS", "192.168.1.10", "User authenticated");
        AuditLog tokenLog = new AuditLog("pranav", "TOKEN_GENERATED", "SUCCESS", "192.168.1.10", "OAuth2 access token issued");
        when(auditLogRepository.findTop100ByOrderByTimestampDesc()).thenReturn(List.of(tokenLog, successLog));
        when(auditLogRepository.findByActionOrderByTimestampDesc("TOKEN_GENERATED")).thenReturn(List.of(tokenLog));

        ResponseEntity<List<AuditLog>> allLogs = auditLogController.getAllLogs();
        assertEquals(2, allLogs.getBody().size());

        ResponseEntity<List<AuditLog>> tokenLogs = auditLogController.getLogsByType("TOKEN_GENERATED");
        assertEquals(1, tokenLogs.getBody().size());
        assertEquals("TOKEN_GENERATED", tokenLogs.getBody().get(0).getAction());

        // Step 5: Programmatic Token Revocation
        ResponseEntity<?> revokeResp = tokenRevocationController.revokeToken("Bearer jwt.sample.token", null);
        assertEquals(200, revokeResp.getStatusCode().value());
        verify(tokenBlacklistService).blacklistToken(eq("jwt.sample.token"), anyLong());
    }

    @Test
    @DisplayName("End-to-End: Strict Rate Limiting (5 allowed, 6th returns 429 with headers)")
    void testRateLimitingEnforcement() throws Exception {
        String clientIp = "172.16.0.42";

        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitingFilter.doFilter(req, res, new MockFilterChain());
            assertEquals(200, res.getStatus());
            assertEquals("5", res.getHeader("X-RateLimit-Limit"));
        }

        // 6th attempt must be throttled with HTTP 429 and Retry-After header
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr(clientIp);
        MockHttpServletResponse res = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(req, res, new MockFilterChain());

        assertEquals(429, res.getStatus());
        assertEquals("60", res.getHeader("Retry-After"));
        assertTrue(res.getContentAsString().contains("Rate limit exceeded"));
    }
}
