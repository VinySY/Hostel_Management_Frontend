package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.AuditLogResponse;
import com.hostelsync.backend.entity.AuditLog;
import com.hostelsync.backend.entity.Complaint;
import com.hostelsync.backend.entity.Student;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.entity.UserRole;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.repository.ComplaintRepository;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    @Mock
    private AuditService auditService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ComplaintRepository complaintRepository;

    @InjectMocks
    private AuditLogController auditLogController;

    private User student1;
    private User student2;
    private UserDetails principal1;
    private UserDetails principal2;
    private Complaint complaint1;
    private AuditLog auditLog1;

    @BeforeEach
    void setUp() {
        student1 = new User();
        student1.setId(101L);
        student1.setEmail("student1@hostelsync.com");
        student1.setName("Student One");
        student1.setRole(UserRole.STUDENT);

        student2 = new User();
        student2.setId(102L);
        student2.setEmail("student2@hostelsync.com");
        student2.setName("Student Two");
        student2.setRole(UserRole.STUDENT);

        principal1 = org.springframework.security.core.userdetails.User
                .withUsername("student1@hostelsync.com").password("pass").roles("STUDENT").build();
        principal2 = org.springframework.security.core.userdetails.User
                .withUsername("student2@hostelsync.com").password("pass").roles("STUDENT").build();

        Student s1 = new Student();
        s1.setId(1L);
        s1.setUser(student1);

        complaint1 = new Complaint();
        complaint1.setId(201L);
        complaint1.setComplaintNumber("CMP-201");
        complaint1.setStudent(s1);

        auditLog1 = new AuditLog();
        auditLog1.setId(1L);
        auditLog1.setEntityType("Complaint");
        auditLog1.setEntityId(201L);
        auditLog1.setAction("STATUS_CHANGE");
        auditLog1.setUserId(101L);
        auditLog1.setUserName("Student One");
        auditLog1.setUserRole("STUDENT");
        auditLog1.setActionTimestamp(LocalDateTime.now());
    }

    @Test
    void getAuditLogsForComplaint_studentOwnComplaint_success() {
        when(userRepository.findByEmail("student1@hostelsync.com")).thenReturn(Optional.of(student1));
        when(complaintRepository.findById(201L)).thenReturn(Optional.of(complaint1));
        when(auditService.getAuditLogsForEntity(eq("Complaint"), eq(201L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(auditLog1)));

        ResponseEntity<Page<AuditLogResponse>> response =
                auditLogController.getAuditLogsForComplaint(201L, 0, 20, principal1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
    }

    @Test
    void getAuditLogsForComplaint_studentOtherComplaint_throwsForbidden() {
        when(userRepository.findByEmail("student2@hostelsync.com")).thenReturn(Optional.of(student2));
        when(complaintRepository.findById(201L)).thenReturn(Optional.of(complaint1));

        assertThrows(ForbiddenException.class, () ->
                auditLogController.getAuditLogsForComplaint(201L, 0, 20, principal2));

        verify(auditService, never()).getAuditLogsForEntity(any(), any(), any());
    }

    @Test
    void getAuditLogsForEntity_studentNonComplaint_throwsForbidden() {
        when(userRepository.findByEmail("student1@hostelsync.com")).thenReturn(Optional.of(student1));

        assertThrows(ForbiddenException.class, () ->
                auditLogController.getAuditLogsForEntity("Hostel", 1L, 0, 20, principal1));

        verify(auditService, never()).getAuditLogsForEntity(any(), any(), any());
    }

    @Test
    void getAuditLogsByUser_ownUser_success() {
        when(userRepository.findByEmail("student1@hostelsync.com")).thenReturn(Optional.of(student1));
        when(auditService.getAuditLogsByUser(eq(101L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(auditLog1)));

        ResponseEntity<Page<AuditLogResponse>> response =
                auditLogController.getAuditLogsByUser(101L, 0, 20, principal1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
    }

    @Test
    void getAuditLogsByUser_otherUser_throwsForbidden() {
        when(userRepository.findByEmail("student1@hostelsync.com")).thenReturn(Optional.of(student1));

        assertThrows(ForbiddenException.class, () ->
                auditLogController.getAuditLogsByUser(102L, 0, 20, principal1));

        verify(auditService, never()).getAuditLogsByUser(any(), any());
    }

    @Test
    void getMyAuditLogs_success() {
        when(userRepository.findByEmail("student1@hostelsync.com")).thenReturn(Optional.of(student1));
        when(auditService.getAuditLogsByUser(eq(101L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(auditLog1)));

        ResponseEntity<Page<AuditLogResponse>> response =
                auditLogController.getMyAuditLogs(0, 20, principal1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
    }
}
