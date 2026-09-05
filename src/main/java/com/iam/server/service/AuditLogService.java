package com.iam.server.service;

import java.util.List;
import com.iam.server.entity.AuditLog;

public interface AuditLogService {

    AuditLog logEvent(String eventType, String username, String ipAddress, String status, String details);

    List<AuditLog> getRecentLogs();

    List<AuditLog> getLogsByUsername(String username);

    List<AuditLog> getLogsByEventType(String eventType);
}
