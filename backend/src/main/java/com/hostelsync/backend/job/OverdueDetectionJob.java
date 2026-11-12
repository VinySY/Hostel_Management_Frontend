package com.hostelsync.backend.job;

import com.hostelsync.backend.entity.Complaint;
import com.hostelsync.backend.entity.ComplaintStatus;
import com.hostelsync.backend.entity.ComplaintTimeline;
import com.hostelsync.backend.repository.ComplaintRepository;
import com.hostelsync.backend.service.AuditService;
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
 * Scheduled job to detect and mark complaints as Overdue when their SLA deadline passes.
 * Runs every 5 minutes.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class OverdueDetectionJob {

    private static final Logger logger = LoggerFactory.getLogger(OverdueDetectionJob.class);

    private final ComplaintRepository complaintRepository;
    private final SlaService slaService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public OverdueDetectionJob(ComplaintRepository complaintRepository,
                               SlaService slaService,
                               NotificationService notificationService,
                               AuditService auditService) {
        this.complaintRepository = complaintRepository;
        this.slaService = slaService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    /**
     * Runs every 5 minutes to check for complaints that have passed their SLA deadline.
     * Runs every five minutes, at second zero.
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void detectOverdueComplaints() {
        logger.info("Starting overdue detection job");

        try {
            // Find all complaints that are not resolved/rejected and have SLA deadline set
            List<Complaint> activeComplaints = complaintRepository.findAll().stream()
                    .filter(c -> c.getStatus() != ComplaintStatus.Resolved
                              && c.getStatus() != ComplaintStatus.Rejected
                              && c.getStatus() != ComplaintStatus.Overdue
                              && c.getSlaDeadline() != null)
                    .collect(Collectors.toList());

            int overdueCount = 0;
            LocalDateTime now = LocalDateTime.now();

            for (Complaint complaint : activeComplaints) {
                if (slaService.isOverdue(complaint.getSlaDeadline())) {
                    // Mark as overdue
                    ComplaintStatus previousStatus = complaint.getStatus();
                    complaint.setStatus(ComplaintStatus.Overdue);
                    complaint.setOverdueAt(now);

                    // Add timeline entry
                    complaint.getTimeline().add(new ComplaintTimeline(
                        complaint,
                        ComplaintStatus.Overdue,
                        "Complaint marked as Overdue - SLA deadline exceeded"
                    ));

                    complaintRepository.save(complaint);

                    // Notify student
                    notificationService.notifyComplaintOverdue(
                        complaint.getStudent().getUser().getId(),
                        complaint.getId(),
                        complaint.getComplaintNumber()
                    );

                    // Notify assigned warden if present
                    if (complaint.getAssignedWarden() != null) {
                        notificationService.notifyComplaintOverdue(
                            complaint.getAssignedWarden().getUser().getId(),
                            complaint.getId(),
                            complaint.getComplaintNumber()
                        );
                    }

                    // Audit log
                    auditService.logComplaintStatusChange(
                        complaint.getId(),
                        previousStatus.toString(),
                        ComplaintStatus.Overdue.toString(),
                        complaint.getStudent().getUser(),
                        "Automatic: SLA deadline exceeded"
                    );

                    overdueCount++;
                    logger.debug("Marked complaint {} as Overdue", complaint.getComplaintNumber());
                }
            }

            logger.info("Overdue detection job completed. Marked {} complaints as overdue", overdueCount);

        } catch (Exception e) {
            logger.error("Error in overdue detection job", e);
        }
    }
}
