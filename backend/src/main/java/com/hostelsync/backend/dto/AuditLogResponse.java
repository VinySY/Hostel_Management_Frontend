package com.hostelsync.backend.dto;

import java.time.LocalDateTime;

/**
 * Response for audit log entries.
 */
public class AuditLogResponse {

    private Long id;
    private String entityType;
    private Long entityId;
    private String action;
    private Long userId;
    private String userName;
    private String userRole;
    private String previousValue;
    private String newValue;
    private String reason;
    private String clientInfo;
    private LocalDateTime timestamp;

    public AuditLogResponse() {}

    public AuditLogResponse(Long id, String entityType, Long entityId, String action,
                           Long userId, String userName, String userRole,
                           String previousValue, String newValue, String reason,
                           String clientInfo, LocalDateTime timestamp) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.userId = userId;
        this.userName = userName;
        this.userRole = userRole;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.reason = reason;
        this.clientInfo = clientInfo;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public void setPreviousValue(String previousValue) {
        this.previousValue = previousValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(String clientInfo) {
        this.clientInfo = clientInfo;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "AuditLogResponse{" +
                "id=" + id +
                ", entityType='" + entityType + '\'' +
                ", entityId=" + entityId +
                ", action='" + action + '\'' +
                ", userName='" + userName + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}