package com.hostelsync.backend.job;

import com.hostelsync.backend.entity.Complaint;
import com.hostelsync.backend.entity.ComplaintStatus;
import com.hostelsync.backend.entity.SlaConfiguration;
import com.hostelsync.backend.repository.ComplaintRepository;
import com.hostelsync.backend.service.NotificationService;
import com.hostelsync.backend.service.SlaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scheduled job to send SLA reminder notifications when complaints are approaching their deadline.
 * Runs every 10 minutes.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class SlaReminderJob {

    private static final Logger logger = LoggerFactory.getLogger(SlaReminderJob.class);

    private final ComplaintRepository complaintRepository;
    private final SlaService slaService;
    private final NotificationService notificationService;

    public SlaReminderJob(ComplaintRepository complaintRepository,
                         SlaService slaService,
                         NotificationService notificationService) {
        this.complaintRepository = complaintRepository;
        this.slaService = slaService;
        this.notificationService = notificationService;
    }

    /**
     * Runs every 10 minutes to check for complaints approaching their SLA deadline.
     * Runs every ten minutes, at second zero.
     */
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void sendSlaReminders() {
        logger.info("Starting SLA reminder job");

        try {
            // Find all active complaints with SLA deadline set and no reminder sent yet
            List<Complaint> activeComplaints = complaintRepository.findAll().stream()
                    .filter(c -> c.getStatus() != ComplaintStatus.Resolved
                              && c.getStatus() != ComplaintStatus.Rejected
                              && c.getStatus() != ComplaintStatus.Overdue
                              && c.getSlaDeadline() != null
                              && c.getReminderSentAt() == null)
                    .collect(Collectors.toList());

            int reminderCount = 0;

            for (Complaint complaint : activeComplaints) {
                // Get threshold for this priority
                SlaConfiguration config = slaService.getConfigurationForPriority(complaint.getPriority());
                int thresholdPercent = config.getReminderThresholdPercent();

                // Check if complaint is approaching deadline
                if (slaService.isApproachingDeadline(
                        complaint.getCreatedAt(),
                        complaint.getSlaDeadline(),
                        thresholdPercent)) {

                    // Send reminder to assigned warden
                    if (complaint.getAssignedWarden() != null) {
                        notificationService.notifyComplaintApproachingDeadline(
                            complaint.getAssignedWarden().getUser().getId(),
                            complaint.getId(),
                            complaint.getComplaintNumber()
                        );
                    }

                    // Also notify student using generic notification
                    notificationService.create(
                        complaint.getStudent().getUser().getId(),
                        com.hostelsync.backend.entity.NotificationType.SLA_REMINDER,
                        "SLA reminder: Your complaint " + complaint.getComplaintNumber() + " is approaching its deadline",
                        "Complaint",
                        complaint.getId()
                    );

                    // Mark reminder as sent
                    complaint.setReminderSentAt(LocalDateTime.now());
                    complaintRepository.save(complaint);

                    reminderCount++;
                    logger.debug("Sent SLA reminder for complaint {}", complaint.getComplaintNumber());
                }
            }

            logger.info("SLA reminder job completed. Sent {} reminders", reminderCount);

        } catch (Exception e) {
            logger.error("Error in SLA reminder job", e);
        }
    }
}
