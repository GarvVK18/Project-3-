package com.iam.server.service;

import com.iam.server.entity.AuditLog;
import com.iam.server.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog logEvent(String username, String action, String status, String clientIp, String details) {
        AuditLog log = new AuditLog(
                username != null ? username : "ANONYMOUS",
                action,
                status,
                clientIp,
                details
        );
        AuditLog saved = auditLogRepository.save(log);
        logger.info("[AUDIT] Event: {}, User: {}, Status: {}, IP: {}, Details: {}",
                action, username, status, clientIp, details);
        return saved;
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByTimestampDesc();
    }

    public List<AuditLog> getLogsForUser(String username) {
        return auditLogRepository.findByUsernameOrderByTimestampDesc(username);
    }
}
