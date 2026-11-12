package com.hostelsync.backend.service;

import com.hostelsync.backend.entity.Notification;
import com.hostelsync.backend.entity.NotificationType;
import com.hostelsync.backend.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Create and persist a new notification.
     */
    public Notification create(Long userId, NotificationType type, String message,
                               String relatedEntityType, Long relatedEntityId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        return notificationRepository.save(notification);
    }

    /**
     * Send general or system announcement notification to a user.
     */
    public void notifyGeneral(Long userId, String title, String message) {
        String fullMsg = (title != null && !title.isBlank()) ? (title + ": " + message) : message;
        create(userId, NotificationType.SYSTEM_ANNOUNCEMENT, fullMsg, "System", null);
    }

    /**
     * Get all notifications for a user (most recent first).
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get unread notifications for a user.
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId);
    }

    /**
     * Count unread notifications for a user.
     */
    @Transactional(readOnly = true)
    public long countUnreadForUser(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    /**
     * Get a notification by its primary key ID.
     */
    @Transactional(readOnly = true)
    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }

    /**
     * Mark a notification as read.
     */
    public boolean markAsRead(Long notificationId) {
        int updated = notificationRepository.markAsRead(notificationId, LocalDateTime.now());
        return updated > 0;
    }

    /**
     * Mark all notifications as read for a user.
     */
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsRead(userId, LocalDateTime.now());
    }

    /**
     * Notify complaint assignment.
     */
    public void notifyComplaintAssigned(Long wardenUserId, Long complaintId, String complaintNumber) {
        create(wardenUserId,
               NotificationType.COMPLAINT_ASSIGNED,
               "You have been assigned complaint " + complaintNumber,
               "Complaint", complaintId);
    }

    /**
     * Notify complaint reassignment.
     */
    public void notifyComplaintReassigned(Long wardenUserId, Long complaintId, String complaintNumber) {
        create(wardenUserId,
               NotificationType.COMPLAINT_REASSIGNED,
               "Complaint " + complaintNumber + " has been reassigned to you",
               "Complaint", complaintId);
    }

    /**
     * Notify complaint status change.
     */
    public void notifyComplaintStatusChanged(Long studentUserId, Long complaintId, String complaintNumber, String newStatus) {
        create(studentUserId,
               NotificationType.COMPLAINT_STATUS_CHANGED,
               "Complaint " + complaintNumber + " status changed to " + newStatus,
               "Complaint", complaintId);
    }

    /**
     * Notify complaint approaching deadline.
     */
    public void notifyComplaintApproachingDeadline(Long wardenUserId, Long complaintId, String complaintNumber) {
        create(wardenUserId,
               NotificationType.COMPLAINT_APPROACHING_DEADLINE,
               "Complaint " + complaintNumber + " is approaching its SLA deadline",
               "Complaint", complaintId);
    }

    /**
     * Notify complaint overdue.
     */
    public void notifyComplaintOverdue(Long wardenUserId, Long complaintId, String complaintNumber) {
        create(wardenUserId,
               NotificationType.COMPLAINT_OVERDUE,
               "Complaint " + complaintNumber + " is now overdue",
               "Complaint", complaintId);
    }

    /**
     * Notify complaint escalation.
     */
    public void notifyComplaintEscalated(Long dgmUserId, Long complaintId, String complaintNumber) {
        create(dgmUserId,
               NotificationType.COMPLAINT_ESCALATED,
               "Complaint " + complaintNumber + " has been escalated to you",
               "Complaint", complaintId);
    }

    /**
     * Notify complaint reopened.
     */
    public void notifyComplaintReopened(Long wardenUserId, Long complaintId, String complaintNumber) {
        create(wardenUserId,
               NotificationType.COMPLAINT_REOPENED,
               "Complaint " + complaintNumber + " has been reopened",
               "Complaint", complaintId);
    }

    /**
     * Notify complaint resolved.
     */
    public void notifyComplaintResolved(Long studentUserId, Long complaintId, String complaintNumber) {
        create(studentUserId,
               NotificationType.COMPLAINT_RESOLVED,
               "Your complaint " + complaintNumber + " has been resolved",
               "Complaint", complaintId);
    }

    /**
     * Notify DGM action taken.
     */
    public void notifyDgmActionTaken(Long wardenUserId, Long complaintId, String complaintNumber, String action) {
        create(wardenUserId,
               NotificationType.DGM_ACTION_TAKEN,
               "DGM took action on complaint " + complaintNumber + ": " + action,
               "Complaint", complaintId);
    }
}
