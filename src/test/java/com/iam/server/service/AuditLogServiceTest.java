package com.iam.server.service;

import com.iam.server.entity.AuditLog;
import com.iam.server.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditLogServiceTest {

    private AuditLogRepository auditLogRepository;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        auditLogService = new AuditLogService(auditLogRepository);
    }

    @Test
    void logEvent_shouldPersistAuditRecord() {
        AuditLog savedLog = new AuditLog("pranav", "LOGIN_SUCCESS", "SUCCESS", "127.0.0.1", "Login OK");
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedLog);

        AuditLog result = auditLogService.logEvent("pranav", "LOGIN_SUCCESS", "SUCCESS", "127.0.0.1", "Login OK");

        assertNotNull(result);
        assertEquals("pranav", result.getUsername());
        assertEquals("LOGIN_SUCCESS", result.getAction());
        assertEquals("SUCCESS", result.getStatus());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void getRecentLogs_shouldReturnLogsFromRepository() {
        AuditLog log1 = new AuditLog("user1", "LOGIN_SUCCESS", "SUCCESS", "127.0.0.1", "OK");
        when(auditLogRepository.findTop100ByOrderByTimestampDesc()).thenReturn(List.of(log1));

        List<AuditLog> logs = auditLogService.getRecentLogs();

        assertEquals(1, logs.size());
        assertEquals("user1", logs.get(0).getUsername());
    }
}
