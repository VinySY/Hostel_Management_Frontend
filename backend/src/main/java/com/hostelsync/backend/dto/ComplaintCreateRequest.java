package com.hostelsync.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON part of the multipart complaint creation request.
 * Sent as the "complaint" part in multipart/form-data.
 */
public class ComplaintCreateRequest {

    /** e.g. "Mess", "Cleaning", "Room Maintenance" */
    @NotBlank(message = "Complaint type is required")
    private String type;

    @NotBlank(message = "Subject is required")
    @Size(max = 300)
    private String subject;

    @NotBlank(message = "Description is required")
    @Size(min = 15, max = 3000, message = "Description must be at least 15 characters")
    private String description;

    @NotBlank(message = "Location is required")
    @Size(max = 300)
    private String location;

    /** Optional — defaults to "Medium" if not provided */
    private String priority;

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
