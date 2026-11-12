package com.hostelsync.backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request to assign a complaint to a warden.
 */
public class ComplaintAssignmentRequest {

    @NotNull(message = "Warden ID is required")
    private Long wardenId;

    private String notes;

    public ComplaintAssignmentRequest() {}

    public ComplaintAssignmentRequest(Long wardenId, String notes) {
        this.wardenId = wardenId;
        this.notes = notes;
    }

    public Long getWardenId() {
        return wardenId;
    }

    public void setWardenId(Long wardenId) {
        this.wardenId = wardenId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "ComplaintAssignmentRequest{" +
                "wardenId=" + wardenId +
                ", notes='" + notes + '\'' +
                '}';
    }
}