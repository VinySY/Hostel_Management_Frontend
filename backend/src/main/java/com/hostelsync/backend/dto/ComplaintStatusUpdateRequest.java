package com.hostelsync.backend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for PATCH /api/complaints/{id}/status
 */
public class ComplaintStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status;     // "Submitted" | "Under Review" | "In Progress" | "Resolved" | "Rejected"

    private String adminNotes;

    private String priority;   // optional override

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
