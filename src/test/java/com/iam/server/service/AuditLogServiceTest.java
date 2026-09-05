package com.iam.server.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.iam.server.entity.AuditLog;
import com.iam.server.repository.AuditLogRepository;

class AuditLogServiceTest {

    private AuditLogRepository auditLogRepository;
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        auditLogService = new AuditLogServiceImpl(auditLogRepository);
    }

    @Test
    void testLogEvent_shouldSaveAndReturnAuditLog() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditLog log = auditLogService.logEvent("LOGIN_SUCCESS", "alice", "192.168.1.1", "SUCCESS", "User login");

        assertNotNull(log);
        assertEquals("LOGIN_SUCCESS", log.getEventType());
        assertEquals("alice", log.getUsername());
        assertEquals("192.168.1.1", log.getIpAddress());
        assertEquals("SUCCESS", log.getStatus());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void testGetRecentLogs_shouldReturnList() {
        AuditLog log1 = new AuditLog("LOGIN_SUCCESS", "bob", "127.0.0.1", "SUCCESS", "ok");
        when(auditLogRepository.findTop100ByOrderByTimestampDesc()).thenReturn(List.of(log1));

        List<AuditLog> logs = auditLogService.getRecentLogs();

        assertEquals(1, logs.size());
        assertEquals("bob", logs.get(0).getUsername());
    }

    @Test
    void testGetLogsByUsername_shouldReturnUserLogs() {
        AuditLog log1 = new AuditLog("TOKEN_ISSUED", "charlie", "127.0.0.1", "SUCCESS", "issued");
        when(auditLogRepository.findByUsernameOrderByTimestampDesc("charlie")).thenReturn(List.of(log1));

        List<AuditLog> logs = auditLogService.getLogsByUsername("charlie");

        assertEquals(1, logs.size());
        assertEquals("charlie", logs.get(0).getUsername());
    }

    @Test
    void testGetLogsByEventType_shouldReturnMatchingLogs() {
        AuditLog log1 = new AuditLog("LOGIN_FAILURE", "unknown", "127.0.0.1", "FAILURE", "bad pass");
        when(auditLogRepository.findByEventTypeOrderByTimestampDesc("LOGIN_FAILURE")).thenReturn(List.of(log1));

        List<AuditLog> logs = auditLogService.getLogsByEventType("LOGIN_FAILURE");

        assertEquals(1, logs.size());
        assertEquals("LOGIN_FAILURE", logs.get(0).getEventType());
    }
}
