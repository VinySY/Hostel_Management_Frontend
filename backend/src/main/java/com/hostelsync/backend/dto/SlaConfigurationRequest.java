package com.hostelsync.backend.dto;

import com.hostelsync.backend.entity.ComplaintPriority;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request to create or update SLA configuration for a priority.
 */
public class SlaConfigurationRequest {

    @NotNull(message = "Priority is required")
    private ComplaintPriority priority;

    @NotNull(message = "Deadline hours is required")
    @Min(value = 1, message = "Deadline hours must be at least 1 hour")
    @Max(value = 720, message = "Deadline hours cannot exceed 720 hours (30 days)")
    private Integer deadlineHours;

    @NotNull(message = "Reminder threshold is required")
    @Min(value = 1, message = "Reminder threshold must be at least 1%")
    @Max(value = 99, message = "Reminder threshold cannot exceed 99%")
    private Integer reminderThresholdPercent;

    public SlaConfigurationRequest() {}

    public SlaConfigurationRequest(ComplaintPriority priority, Integer deadlineHours, Integer reminderThresholdPercent) {
        this.priority = priority;
        this.deadlineHours = deadlineHours;
        this.reminderThresholdPercent = reminderThresholdPercent;
    }

    public ComplaintPriority getPriority() {
        return priority;
    }

    public void setPriority(ComplaintPriority priority) {
        this.priority = priority;
    }

    public Integer getDeadlineHours() {
        return deadlineHours;
    }

    public void setDeadlineHours(Integer deadlineHours) {
        this.deadlineHours = deadlineHours;
    }

    public Integer getReminderThresholdPercent() {
        return reminderThresholdPercent;
    }

    public void setReminderThresholdPercent(Integer reminderThresholdPercent) {
        this.reminderThresholdPercent = reminderThresholdPercent;
    }

    @Override
    public String toString() {
        return "SlaConfigurationRequest{" +
                "priority=" + priority +
                ", deadlineHours=" + deadlineHours +
                ", reminderThresholdPercent=" + reminderThresholdPercent +
                '}';
    }
}