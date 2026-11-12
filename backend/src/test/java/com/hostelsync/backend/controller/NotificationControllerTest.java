package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.NotificationResponse;
import com.hostelsync.backend.entity.Notification;
import com.hostelsync.backend.entity.NotificationType;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.entity.UserRole;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationController notificationController;

    private User user1;
    private User user2;
    private UserDetails principal1;
    private UserDetails principal2;
    private Notification notification1;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(10L);
        user1.setEmail("user1@hostelsync.com");
        user1.setName("User One");
        user1.setRole(UserRole.STUDENT);

        user2 = new User();
        user2.setId(20L);
        user2.setEmail("user2@hostelsync.com");
        user2.setName("User Two");
        user2.setRole(UserRole.STUDENT);

        principal1 = org.springframework.security.core.userdetails.User
                .withUsername("user1@hostelsync.com").password("pass").roles("STUDENT").build();
        principal2 = org.springframework.security.core.userdetails.User
                .withUsername("user2@hostelsync.com").password("pass").roles("STUDENT").build();

        notification1 = new Notification();
        notification1.setId(100L);
        notification1.setUserId(10L);
        notification1.setType(NotificationType.COMPLAINT_STATUS_CHANGED);
        notification1.setMessage("Your complaint status has changed");
        notification1.setRelatedEntityType("Complaint");
        notification1.setRelatedEntityId(500L);
        notification1.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getNotifications_returnsUserNotifications() {
        when(userRepository.findByEmail("user1@hostelsync.com")).thenReturn(Optional.of(user1));
        when(notificationService.getNotificationsForUser(10L)).thenReturn(List.of(notification1));

        ResponseEntity<List<NotificationResponse>> response = notificationController.getNotifications(principal1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(100L, response.getBody().get(0).getId());
        assertEquals("Status Updated", response.getBody().get(0).getTitle());
    }

    @Test
    void countUnreadNotifications_returnsCount() {
        when(userRepository.findByEmail("user1@hostelsync.com")).thenReturn(Optional.of(user1));
        when(notificationService.countUnreadForUser(10L)).thenReturn(3L);

        ResponseEntity<Long> response = notificationController.countUnreadNotifications(principal1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3L, response.getBody());
    }

    @Test
    void getNotification_ownNotification_returnsNotification() {
        when(userRepository.findByEmail("user1@hostelsync.com")).thenReturn(Optional.of(user1));
        when(notificationService.getNotificationById(100L)).thenReturn(Optional.of(notification1));

        ResponseEntity<NotificationResponse> response = notificationController.getNotification(100L, principal1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(100L, response.getBody().getId());
    }

    @Test
    void getNotification_otherUserNotification_throwsForbidden() {
        when(userRepository.findByEmail("user2@hostelsync.com")).thenReturn(Optional.of(user2));
        when(notificationService.getNotificationById(100L)).thenReturn(Optional.of(notification1));

        assertThrows(ForbiddenException.class, () ->
                notificationController.getNotification(100L, principal2));
    }

    @Test
    void getNotification_notFound_throwsResourceNotFound() {
        when(userRepository.findByEmail("user1@hostelsync.com")).thenReturn(Optional.of(user1));
        when(notificationService.getNotificationById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                notificationController.getNotification(999L, principal1));
    }

    @Test
    void markAsRead_ownNotification_success() {
        when(userRepository.findByEmail("user1@hostelsync.com")).thenReturn(Optional.of(user1));
        when(notificationService.getNotificationById(100L)).thenReturn(Optional.of(notification1));
        when(notificationService.markAsRead(100L)).thenReturn(true);

        ResponseEntity<Void> response = notificationController.markAsRead(100L, principal1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService).markAsRead(100L);
    }

    @Test
    void markAsRead_otherUserNotification_throwsForbidden() {
        when(userRepository.findByEmail("user2@hostelsync.com")).thenReturn(Optional.of(user2));
        when(notificationService.getNotificationById(100L)).thenReturn(Optional.of(notification1));

        assertThrows(ForbiddenException.class, () ->
                notificationController.markAsRead(100L, principal2));

        verify(notificationService, never()).markAsRead(anyLong());
    }

    @Test
    void markAllAsRead_success() {
        when(userRepository.findByEmail("user1@hostelsync.com")).thenReturn(Optional.of(user1));
        when(notificationService.markAllAsRead(10L)).thenReturn(5);

        ResponseEntity<Integer> response = notificationController.markAllAsRead(principal1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5, response.getBody());
        verify(notificationService).markAllAsRead(10L);
    }
}
