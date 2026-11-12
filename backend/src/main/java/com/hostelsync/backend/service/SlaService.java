package com.hostelsync.backend.service;

import com.hostelsync.backend.entity.ComplaintPriority;
import com.hostelsync.backend.entity.SlaConfiguration;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.exception.ConflictException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.SlaConfigurationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SlaService {

    private final SlaConfigurationRepository slaConfigurationRepository;

    public SlaService(SlaConfigurationRepository slaConfigurationRepository) {
        this.slaConfigurationRepository = slaConfigurationRepository;
    }

    /**
     * Get SLA configuration for a priority level.
     * Returns in-memory default if not configured in DB.
     */
    @Transactional(readOnly = true)
    public SlaConfiguration getConfigurationForPriority(ComplaintPriority priority) {
        return slaConfigurationRepository.findByPriority(priority)
                .orElseGet(() -> getDefaultConfiguration(priority));
    }

    /**
     * Calculate SLA deadline based on priority and current time.
     */
    @Transactional(readOnly = true)
    public LocalDateTime calculateDeadline(ComplaintPriority priority, LocalDateTime startTime) {
        SlaConfiguration config = getConfigurationForPriority(priority);
        return startTime.plusHours(config.getDeadlineHours());
    }

    /**
     * Check if a complaint is overdue based on its SLA deadline.
     */
    @Transactional(readOnly = true)
    public boolean isOverdue(LocalDateTime slaDeadline) {
        if (slaDeadline == null) return false;
        return LocalDateTime.now().isAfter(slaDeadline);
    }

    /**
     * Check if a complaint is approaching its deadline.
     * @param thresholdPercent percentage of time remaining below which is "approaching" (e.g., 50)
     */
    @Transactional(readOnly = true)
    public boolean isApproachingDeadline(LocalDateTime createdAt, LocalDateTime slaDeadline, int thresholdPercent) {
        if (slaDeadline == null || createdAt == null) return false;

        long totalMinutes = java.time.temporal.ChronoUnit.MINUTES.between(createdAt, slaDeadline);
        long remainingMinutes = java.time.temporal.ChronoUnit.MINUTES.between(LocalDateTime.now(), slaDeadline);

        if (totalMinutes <= 0) return false;
        double percentRemaining = (double) remainingMinutes / totalMinutes * 100;
        return percentRemaining <= thresholdPercent;
    }

    /**
     * Get all SLA configurations.
     */
    @Transactional(readOnly = true)
    public List<SlaConfiguration> getAllConfigurations() {
        return slaConfigurationRepository.findAll();
    }

    /**
     * Update or create SLA configuration for a priority.
     */
    public SlaConfiguration upsertConfiguration(ComplaintPriority priority, Integer deadlineHours, Integer reminderThresholdPercent) {
        if (priority == null) {
            throw new BadRequestException("Priority is required");
        }
        if (deadlineHours == null) {
            throw new BadRequestException("Deadline hours is required");
        }
        validateSlaValues(deadlineHours, reminderThresholdPercent);

        SlaConfiguration config = slaConfigurationRepository.findByPriority(priority)
                .orElse(new SlaConfiguration());

        config.setPriority(priority);
        config.setDeadlineHours(deadlineHours);

        if (reminderThresholdPercent != null) {
            config.setReminderThresholdPercent(reminderThresholdPercent);
        }

        return slaConfigurationRepository.save(config);
    }

    /**
     * Get SLA configuration by ID.
     */
    @Transactional(readOnly = true)
    public SlaConfiguration getById(Long id) {
        return slaConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SLA Configuration", id));
    }

    /**
     * Create a new SLA configuration.
     * Throws ConflictException if a configuration for the priority already exists.
     */
    public SlaConfiguration createConfiguration(ComplaintPriority priority, Integer deadlineHours, Integer reminderThresholdPercent) {
        if (priority == null) {
            throw new BadRequestException("Priority is required");
        }
        validateSlaValues(deadlineHours, reminderThresholdPercent);
        if (slaConfigurationRepository.findByPriority(priority).isPresent()) {
            throw new ConflictException("SLA configuration already exists for priority: " + priority);
        }

        SlaConfiguration config = new SlaConfiguration(priority, deadlineHours != null ? deadlineHours : 24);
        if (reminderThresholdPercent != null) {
            config.setReminderThresholdPercent(reminderThresholdPercent);
        }
        return slaConfigurationRepository.save(config);
    }

    /**
     * Update SLA configuration by ID.
     */
    public SlaConfiguration updateConfiguration(Long id, ComplaintPriority priority, Integer deadlineHours, Integer reminderThresholdPercent) {
        validateSlaValues(deadlineHours, reminderThresholdPercent);
        SlaConfiguration config = getById(id);

        if (priority != null && priority != config.getPriority()) {
            java.util.Optional<SlaConfiguration> existing = slaConfigurationRepository.findByPriority(priority);
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new ConflictException("SLA configuration already exists for priority: " + priority);
            }
            config.setPriority(priority);
        }

        if (deadlineHours != null) {
            config.setDeadlineHours(deadlineHours);
        }

        if (reminderThresholdPercent != null) {
            config.setReminderThresholdPercent(reminderThresholdPercent);
        }

        return slaConfigurationRepository.save(config);
    }

    /**
     * Get in-memory default SLA configuration based on priority.
     *   Urgent: 2 hours
     *   High:   8 hours
     *   Medium: 24 hours
     *   Low:    48 hours
     * All use 50% threshold for reminders.
     */
    private static void validateSlaValues(Integer deadlineHours, Integer reminderThresholdPercent) {
        if (deadlineHours != null && deadlineHours < 0) {
            throw new BadRequestException("Deadline hours must be 0 or greater");
        }
        if (reminderThresholdPercent != null && reminderThresholdPercent < 0) {
            throw new BadRequestException("Reminder threshold percent must be 0 or greater");
        }
    }

    private SlaConfiguration getDefaultConfiguration(ComplaintPriority priority) {
        SlaConfiguration config = new SlaConfiguration();
        config.setPriority(priority);
        config.setReminderThresholdPercent(50);

        switch (priority) {
            case Urgent -> config.setDeadlineHours(2);
            case High   -> config.setDeadlineHours(8);
            case Medium -> config.setDeadlineHours(24);
            case Low    -> config.setDeadlineHours(48);
        }
        return config;
    }
}
