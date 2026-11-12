package com.hostelsync.backend.dto;

import com.hostelsync.backend.entity.EscalationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Request to escalate a complaint to DGM.
 */
public class EscalationRequest {

    @NotBlank(message = "Reason is required")
    @Size(min = 10, max = 500, message = "Reason must be 10-500 characters")
    private String reason;

    @NotBlank(message = "Details are required")
    @Size(min = 10, max = 1000, message = "Details must be 10-1000 characters")
    private String details;

    public EscalationRequest() {}

    public EscalationRequest(String reason, String details) {
        this.reason = reason;
        this.details = details;
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

    @Override
    public String toString() {
        return "EscalationRequest{" +
                "reason='" + reason + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}
