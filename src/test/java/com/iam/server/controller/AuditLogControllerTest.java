package com.iam.server.controller;

import com.iam.server.entity.AuditLog;
import com.iam.server.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AuditLogControllerTest {

    private AuditLogService auditLogService;
    private AuditLogController auditLogController;

    @BeforeEach
    void setUp() {
        auditLogService = mock(AuditLogService.class);
        auditLogController = new AuditLogController(auditLogService);
    }

    @Test
    void getAllLogs_shouldReturnRecentLogs() {
        AuditLog log1 = new AuditLog("pranav", "LOGIN_SUCCESS", "SUCCESS", "127.0.0.1", "OK");
        when(auditLogService.getRecentLogs()).thenReturn(List.of(log1));

        ResponseEntity<List<AuditLog>> response = auditLogController.getAllLogs();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("LOGIN_SUCCESS", response.getBody().get(0).getAction());
        verify(auditLogService).getRecentLogs();
    }

    @Test
    void getLogsByUser_shouldFilterByUsername() {
        AuditLog log1 = new AuditLog("pranav", "TOKEN_GENERATED", "SUCCESS", "127.0.0.1", "Token OK");
        when(auditLogService.getLogsForUser("pranav")).thenReturn(List.of(log1));

        ResponseEntity<List<AuditLog>> response = auditLogController.getLogsByUser("pranav");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("pranav", response.getBody().get(0).getUsername());
        verify(auditLogService).getLogsForUser("pranav");
    }

    @Test
    void getLogsByType_shouldFilterByEventType() {
        AuditLog log1 = new AuditLog("pranav", "LOGIN_FAILURE", "FAILURE", "127.0.0.1", "Bad credentials");
        when(auditLogService.getLogsByType("LOGIN_FAILURE")).thenReturn(List.of(log1));

        ResponseEntity<List<AuditLog>> response = auditLogController.getLogsByType("LOGIN_FAILURE");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("LOGIN_FAILURE", response.getBody().get(0).getAction());
        verify(auditLogService).getLogsByType("LOGIN_FAILURE");
    }
}
