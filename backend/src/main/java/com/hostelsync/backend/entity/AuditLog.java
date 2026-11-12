package com.hostelsync.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Audit log for tracking important business actions.
 * Provides an immutable record of who did what and when.
 */
@Entity
@Table(name = "audit_logs",
       indexes = {
           @Index(columnList = "entity_type,entity_id"),
           @Index(columnList = "action_timestamp"),
           @Index(columnList = "user_id")
       })
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @NotNull
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @NotBlank
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /** User who performed the action */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name", nullable = false, length = 150)
    private String userName;

    @Column(name = "user_role", nullable = false, length = 20)
    private String userRole;

    /** Previous value (JSON or text) */
    @Column(name = "previous_value", columnDefinition = "TEXT")
    private String previousValue;

    /** New value (JSON or text) */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /** Additional context or reason */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /** IP address or other client info */
    @Column(name = "client_info", length = 200)
    private String clientInfo;

    @Column(name = "action_timestamp", nullable = false, updatable = false)
    private LocalDateTime actionTimestamp;

    @PrePersist
    protected void onCreate() {
        actionTimestamp = LocalDateTime.now();
    }

    // -------- Constructors --------

    public AuditLog() {}

    public AuditLog(String entityType, Long entityId, String action, Long userId, String userName, String userRole) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.userId = userId;
        this.userName = userName;
        this.userRole = userRole;
    }

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getPreviousValue() { return previousValue; }
    public void setPreviousValue(String previousValue) { this.previousValue = previousValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getClientInfo() { return clientInfo; }
    public void setClientInfo(String clientInfo) { this.clientInfo = clientInfo; }

    public LocalDateTime getActionTimestamp() { return actionTimestamp; }
    public void setActionTimestamp(LocalDateTime actionTimestamp) { this.actionTimestamp = actionTimestamp; }
}
