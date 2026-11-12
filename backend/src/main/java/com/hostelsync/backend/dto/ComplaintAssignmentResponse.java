package com.hostelsync.backend.dto;

import java.time.LocalDateTime;

/**
 * Response for complaint assignment information.
 */
public class ComplaintAssignmentResponse {

    private Long id;
    private Long complaintId;
    private String complaintNumber;
    private Long assignedWardenId;
    private String assignedWardenName;
    private Long assignedById;
    private String assignedByName;
    private String notes;
    private LocalDateTime assignedAt;

    public ComplaintAssignmentResponse() {}

    public ComplaintAssignmentResponse(Long id, Long complaintId, String complaintNumber,
                                      Long assignedWardenId, String assignedWardenName,
                                      Long assignedById, String assignedByName,
                                      String notes, LocalDateTime assignedAt) {
        this.id = id;
        this.complaintId = complaintId;
        this.complaintNumber = complaintNumber;
        this.assignedWardenId = assignedWardenId;
        this.assignedWardenName = assignedWardenName;
        this.assignedById = assignedById;
        this.assignedByName = assignedByName;
        this.notes = notes;
        this.assignedAt = assignedAt;
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

    public Long getAssignedWardenId() {
        return assignedWardenId;
    }

    public void setAssignedWardenId(Long assignedWardenId) {
        this.assignedWardenId = assignedWardenId;
    }

    public String getAssignedWardenName() {
        return assignedWardenName;
    }

    public void setAssignedWardenName(String assignedWardenName) {
        this.assignedWardenName = assignedWardenName;
    }

    public Long getAssignedById() {
        return assignedById;
    }

    public void setAssignedById(Long assignedById) {
        this.assignedById = assignedById;
    }

    public String getAssignedByName() {
        return assignedByName;
    }

    public void setAssignedByName(String assignedByName) {
        this.assignedByName = assignedByName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    @Override
    public String toString() {
        return "ComplaintAssignmentResponse{" +
                "id=" + id +
                ", complaintId=" + complaintId +
                ", complaintNumber='" + complaintNumber + '\'' +
                ", assignedWardenName='" + assignedWardenName + '\'' +
                ", assignedAt=" + assignedAt +
                '}';
    }
}