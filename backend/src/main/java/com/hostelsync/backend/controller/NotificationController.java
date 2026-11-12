package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.NotificationResponse;
import com.hostelsync.backend.entity.Notification;
import com.hostelsync.backend.entity.NotificationType;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Notification endpoints for authenticated users.
 * Users can only access and modify their own notifications.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /**
     * GET /api/notifications - Get notifications for authenticated user
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());
        List<Notification> notifications = notificationService.getNotificationsForUser(caller.getId());

        List<NotificationResponse> responses = notifications.stream()
                .map(this::toNotificationResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/notifications/unread - Get unread notifications for authenticated user
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());
        List<Notification> notifications = notificationService.getUnreadNotificationsForUser(caller.getId());

        List<NotificationResponse> responses = notifications.stream()
                .map(this::toNotificationResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/notifications/unread/count - Count unread notifications
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Long> countUnreadNotifications(
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());
        long count = notificationService.countUnreadForUser(caller.getId());

        return ResponseEntity.ok(count);
    }

    /**
     * POST or PUT /api/notifications/{id}/read or PUT /api/notifications/{id} - Mark notification as read
     */
    @RequestMapping(value = {"/{id}/read", "/{id}"}, method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());

        // Verify the notification exists and belongs to the current user (throws 403 if other user's)
        getNotificationForUser(id, caller.getId());

        boolean updated = notificationService.markAsRead(id);
        if (!updated) {
            throw new ResourceNotFoundException("Notification", id);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/notifications/mark-all-read - Mark all notifications as read for current user
     */
    @PostMapping("/mark-all-read")
    public ResponseEntity<Integer> markAllAsRead(
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());
        int updated = notificationService.markAllAsRead(caller.getId());

        return ResponseEntity.ok(updated);
    }

    /**
     * GET /api/notifications/{id} - Get specific notification
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = findUser(principal.getUsername());

        // Verify the notification belongs to the current user (throws 403 if other user's)
        Notification notification = getNotificationForUser(id, caller.getId());

        return ResponseEntity.ok(toNotificationResponse(notification));
    }

    /**
     * Get notification by ID and verify ownership.
     * Throws ResourceNotFoundException (404) if notification does not exist.
     * Throws ForbiddenException (403) if notification belongs to another user.
     */
    private Notification getNotificationForUser(Long notificationId, Long userId) {
        Notification notification = notificationService.getNotificationById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied to notification");
        }

        return notification;
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                formatTitle(notification.getType()),
                notification.getMessage(),
                notification.getRelatedEntityType(),
                notification.getRelatedEntityId(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }

    private String formatTitle(NotificationType type) {
        if (type == null) return "Notification";
        return switch (type) {
            case COMPLAINT_ASSIGNED -> "Complaint Assigned";
            case COMPLAINT_REASSIGNED -> "Complaint Reassigned";
            case COMPLAINT_STATUS_CHANGED -> "Status Updated";
            case COMPLAINT_APPROACHING_DEADLINE -> "SLA Deadline Warning";
            case COMPLAINT_OVERDUE -> "Complaint Overdue";
            case COMPLAINT_ESCALATED -> "Complaint Escalated";
            case COMPLAINT_REOPENED -> "Complaint Reopened";
            case COMPLAINT_RESOLVED -> "Complaint Resolved";
            case DGM_ACTION_TAKEN -> "DGM Action";
            default -> "Notification";
        };
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
