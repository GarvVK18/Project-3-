package com.iam.server.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iam.server.entity.AuditLog;
import com.iam.server.repository.AuditLogRepository;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public AuditLog logEvent(String eventType, String username, String ipAddress, String status, String details) {
        AuditLog auditLog = new AuditLog(eventType, username != null ? username : "ANONYMOUS", ipAddress, status, details);
        log.info("[AUDIT] Type: {} | User: {} | IP: {} | Status: {} | Details: {}",
                eventType, username, ipAddress, status, details);
        return auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByTimestampDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByUsername(String username) {
        return auditLogRepository.findByUsernameOrderByTimestampDesc(username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByEventType(String eventType) {
        return auditLogRepository.findByEventTypeOrderByTimestampDesc(eventType);
    }
}
