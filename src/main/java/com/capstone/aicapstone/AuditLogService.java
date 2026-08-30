package com.capstone.aicapstone;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String actorEmail, String actorRole, String action, String details, String ipAddress) {
        try {
            String now = LocalDateTime.now().format(FORMATTER);
            AuditLog auditLog = new AuditLog(now, actorEmail, actorRole, action, details, ipAddress != null ? ipAddress : "127.0.0.1");
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            System.err.println("AuditLogService warning: Failed to record audit log: " + e.getMessage());
        }
    }

    public void log(String actorEmail, String actorRole, String action, String details) {
        log(actorEmail, actorRole, action, details, "127.0.0.1");
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop100ByOrderByIdDesc();
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByIdDesc();
    }
}
