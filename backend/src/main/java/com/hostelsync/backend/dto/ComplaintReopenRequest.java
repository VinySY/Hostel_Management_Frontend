package com.hostelsync.backend.dto;

import jakarta.validation.constraints.Size;

/**
 * Request for reopening a complaint.
 * Reason is optional but can provide context for the reopening.
 */
public class ComplaintReopenRequest {

    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    private String reason;

    public ComplaintReopenRequest() {}

    public ComplaintReopenRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "ComplaintReopenRequest{" +
                "reason='" + (reason != null ? reason : "") + '\'' +
                '}';
    }
}