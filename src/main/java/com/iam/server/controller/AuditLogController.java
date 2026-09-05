package com.iam.server.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.iam.server.entity.AuditLog;
import com.iam.server.service.AuditLogService;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<AuditLog>> getRecentAuditLogs() {
        return ResponseEntity.ok(auditLogService.getRecentLogs());
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByUser(@PathVariable String username) {
        return ResponseEntity.ok(auditLogService.getLogsByUsername(username));
    }

    @GetMapping("/type/{eventType}")
    public ResponseEntity<List<AuditLog>> getAuditLogsByType(@PathVariable String eventType) {
        return ResponseEntity.ok(auditLogService.getLogsByEventType(eventType));
    }
}
