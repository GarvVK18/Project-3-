package com.iam.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.iam.server.entity.AuditLog;
import com.iam.server.service.AuditLogService;

class AuditLogControllerTest {

    private AuditLogService auditLogService;
    private AuditLogController controller;

    @BeforeEach
    void setUp() {
        auditLogService = mock(AuditLogService.class);
        controller = new AuditLogController(auditLogService);
    }

    @Test
    void testGetRecentAuditLogs_shouldReturnList() {
        AuditLog log = new AuditLog("LOGIN_SUCCESS", "alice", "127.0.0.1", "SUCCESS", "ok");
        when(auditLogService.getRecentLogs()).thenReturn(List.of(log));

        ResponseEntity<List<AuditLog>> response = controller.getRecentAuditLogs();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("alice", response.getBody().get(0).getUsername());
    }

    @Test
    void testGetAuditLogsByUser_shouldReturnUserLogs() {
        AuditLog log = new AuditLog("LOGIN_SUCCESS", "bob", "127.0.0.1", "SUCCESS", "ok");
        when(auditLogService.getLogsByUsername("bob")).thenReturn(List.of(log));

        ResponseEntity<List<AuditLog>> response = controller.getAuditLogsByUser("bob");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("bob", response.getBody().get(0).getUsername());
    }

    @Test
    void testGetAuditLogsByType_shouldReturnTypedLogs() {
        AuditLog log = new AuditLog("TOKEN_REVOKED", "charlie", "127.0.0.1", "SUCCESS", "revoked");
        when(auditLogService.getLogsByEventType("TOKEN_REVOKED")).thenReturn(List.of(log));

        ResponseEntity<List<AuditLog>> response = controller.getAuditLogsByType("TOKEN_REVOKED");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("TOKEN_REVOKED", response.getBody().get(0).getEventType());
    }
}
