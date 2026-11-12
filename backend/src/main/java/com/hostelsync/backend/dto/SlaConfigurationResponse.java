package com.hostelsync.backend.dto;

import com.hostelsync.backend.entity.ComplaintPriority;

/**
 * Response for SLA configuration.
 */
public class SlaConfigurationResponse {

    private Long id;
    private ComplaintPriority priority;
    private Integer deadlineHours;
    private Integer reminderThresholdPercent;

    public SlaConfigurationResponse() {}

    public SlaConfigurationResponse(Long id, ComplaintPriority priority, Integer deadlineHours, Integer reminderThresholdPercent) {
        this.id = id;
        this.priority = priority;
        this.deadlineHours = deadlineHours;
        this.reminderThresholdPercent = reminderThresholdPercent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        return "SlaConfigurationResponse{" +
                "id=" + id +
                ", priority=" + priority +
                ", deadlineHours=" + deadlineHours +
                ", reminderThresholdPercent=" + reminderThresholdPercent +
                '}';
    }
}