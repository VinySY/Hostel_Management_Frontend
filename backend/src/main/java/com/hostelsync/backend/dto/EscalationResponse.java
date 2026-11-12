package com.hostelsync.backend.dto;

import com.hostelsync.backend.entity.EscalationStatus;
import java.time.LocalDateTime;

/**
 * Response for escalation information.
 */
public class EscalationResponse {

    private Long id;
    private Long complaintId;
    private String complaintNumber;
    private Long escalatedByStudentId;
    private String escalatedByStudentName;
    private String reason;
    private String details;
    private EscalationStatus status;
    private Long handledByDgmId;
    private String handledByDgmName;
    private String dgmResponse;
    private LocalDateTime escalatedAt;
    private LocalDateTime handledAt;

    public EscalationResponse() {}

    public EscalationResponse(Long id, Long complaintId, String complaintNumber, Long escalatedByStudentId,
                             String escalatedByStudentName, String reason, String details, EscalationStatus status,
                             Long handledByDgmId, String handledByDgmName, String dgmResponse,
                             LocalDateTime escalatedAt, LocalDateTime handledAt) {
        this.id = id;
        this.complaintId = complaintId;
        this.complaintNumber = complaintNumber;
        this.escalatedByStudentId = escalatedByStudentId;
        this.escalatedByStudentName = escalatedByStudentName;
        this.reason = reason;
        this.details = details;
        this.status = status;
        this.handledByDgmId = handledByDgmId;
        this.handledByDgmName = handledByDgmName;
        this.dgmResponse = dgmResponse;
        this.escalatedAt = escalatedAt;
        this.handledAt = handledAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(Long complaintId) {
        this.complaintId = complaintId;
    }

    public String getComplaintNumber() {
        return complaintNumber;
    }

    public void setComplaintNumber(String complaintNumber) {
        this.complaintNumber = complaintNumber;
    }

    public Long getEscalatedByStudentId() {
        return escalatedByStudentId;
    }

    public void setEscalatedByStudentId(Long escalatedByStudentId) {
        this.escalatedByStudentId = escalatedByStudentId;
    }

    public String getEscalatedByStudentName() {
        return escalatedByStudentName;
    }

    public void setEscalatedByStudentName(String escalatedByStudentName) {
        this.escalatedByStudentName = escalatedByStudentName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public EscalationStatus getStatus() {
        return status;
    }

    public void setStatus(EscalationStatus status) {
        this.status = status;
    }

    public Long getHandledByDgmId() {
        return handledByDgmId;
    }

    public void setHandledByDgmId(Long handledByDgmId) {
        this.handledByDgmId = handledByDgmId;
    }

    public String getHandledByDgmName() {
        return handledByDgmName;
    }

    public void setHandledByDgmName(String handledByDgmName) {
        this.handledByDgmName = handledByDgmName;
    }

    public String getDgmResponse() {
        return dgmResponse;
    }

    public void setDgmResponse(String dgmResponse) {
        this.dgmResponse = dgmResponse;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    @Override
    public String toString() {
        return "EscalationResponse{" +
                "id=" + id +
                ", complaintId=" + complaintId +
                ", complaintNumber='" + complaintNumber + '\'' +
                ", reason='" + reason + '\'' +
                ", status=" + status +
                ", escalatedAt=" + escalatedAt +
                ", handledAt=" + handledAt +
                '}';
    }
}