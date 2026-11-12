package com.hostelsync.backend.service;

import com.hostelsync.backend.entity.AuditLog;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Create an audit log entry for an action.
     */
    public AuditLog log(String entityType, Long entityId, String action, User user,
                        String previousValue, String newValue, String reason, String clientInfo) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setUserId(user.getId());
        log.setUserName(user.getName());
        log.setUserRole(user.getRole().toString());
        log.setPreviousValue(previousValue);
        log.setNewValue(newValue);
        log.setReason(reason);
        log.setClientInfo(clientInfo);
        return auditLogRepository.save(log);
    }

    /**
     * Log a complaint-related action.
     */
    public AuditLog logComplaintAction(Long complaintId, String action, User user, String reason) {
        return log("Complaint", complaintId, action, user, null, null, reason, null);
    }

    /**
     * Log a complaint status change.
     */
    public AuditLog logComplaintStatusChange(Long complaintId, String previousStatus, String newStatus,
                                             User user, String reason) {
        return log("Complaint", complaintId, "STATUS_CHANGED", user, previousStatus, newStatus, reason, null);
    }

    /**
     * Log a complaint assignment.
     */
    public AuditLog logComplaintAssignment(Long complaintId, Long wardenId, User user, String reason) {
        return log("Complaint", complaintId, "ASSIGNED", user, null, "warden:" + wardenId, reason, null);
    }

    /**
     * Log a complaint escalation.
     */
    public AuditLog logComplaintEscalation(Long complaintId, User user, String reason) {
        return log("Complaint", complaintId, "ESCALATED", user, null, null, reason, null);
    }

    /**
     * Log a complaint reopening.
     */
    public AuditLog logComplaintReopening(Long complaintId, User user, String reason) {
        return log("Complaint", complaintId, "REOPENED", user, null, null, reason, null);
    }

    /**
     * Log a room allocation action.
     */
    public AuditLog logRoomAllocationAction(Long studentId, String action, User user, String reason) {
        return log("RoomAllocation", studentId, action, user, null, null, reason, null);
    }

    /**
     * Get audit logs for an entity.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsForEntity(String entityType, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }

    /**
     * Get audit logs by user.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Get audit logs by entity type.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByEntityType(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityType(entityType, pageable);
    }

    /**
     * Get audit logs within a time range.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByTimeRange(LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return auditLogRepository.findByTimestampRange(from, to, pageable);
    }
}
