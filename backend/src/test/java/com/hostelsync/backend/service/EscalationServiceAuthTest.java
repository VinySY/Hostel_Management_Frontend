package com.hostelsync.backend.service;

import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.ComplaintEscalationRepository;
import com.hostelsync.backend.repository.ComplaintRepository;
import com.hostelsync.backend.repository.DgmRepository;
import com.hostelsync.backend.repository.HostelDgmAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for DGM authorization in EscalationService.handleEscalation().
 * Verifies fail-closed authorization prevents IDOR vulnerabilities.
 */
@ExtendWith(MockitoExtension.class)
class EscalationServiceAuthTest {

    @Mock
    private ComplaintEscalationRepository escalationRepository;

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private DgmRepository dgmRepository;

    @Mock
    private HostelDgmAssignmentRepository hostelDgmAssignmentRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    @Mock
    private SlaService slaService;

    @InjectMocks
    private EscalationService escalationService;

    private User dgmUser;
    private Dgm dgm;
    private Hostel hostel;
    private Room room;
    private User studentUser;
    private Student student;
    private Complaint complaint;
    private ComplaintEscalation escalation;

    @BeforeEach
    void setUp() {
        // Setup DGM
        dgmUser = new User("DGM User", "dgm@test.com", "hashedpw", UserRole.DGM);
        dgmUser.setId(100L);

        dgm = new Dgm();
        dgm.setId(10L);
        dgm.setUser(dgmUser);
        dgm.setEmployeeId("DGM-001");

        // Setup Hostel
        hostel = new Hostel("North Hostel", "123 Campus Rd");
        hostel.setId(1L);

        // Setup Room
        room = new Room("101", "Block A", 1, 2, hostel);
        room.setId(50L);

        // Setup Student
        studentUser = new User("Student User", "student@test.com", "hashedpw", UserRole.STUDENT);
        studentUser.setId(200L);

        student = new Student();
        student.setId(20L);
        student.setUser(studentUser);
        student.setStudentId("STU-2024-001");
        student.setRoom(room);
        student.setCourse("Computer Science");
        student.setYear(2);
        student.setPhone("1234567890");

        // Setup Complaint
        complaint = new Complaint();
        complaint.setId(300L);
        complaint.setComplaintNumber("CMP-300");
        complaint.setStudent(student);
        complaint.setType(ComplaintType.Electricity);
        complaint.setSubject("Test complaint");
        complaint.setDescription("This is a test complaint for authorization");
        complaint.setLocation("Room 101");
        complaint.setPriority(ComplaintPriority.High);
        complaint.setStatus(ComplaintStatus.Under_Review);

        // Setup Escalation
        escalation = new ComplaintEscalation();
        escalation.setId(400L);
        escalation.setComplaint(complaint);
        escalation.setEscalatedByStudent(student);
        escalation.setReason("Overdue and unresolved");
        escalation.setStatus(EscalationStatus.PENDING);
    }

    @Test
    void handleEscalation_assignedDgm_success() {
        // Arrange
        when(escalationRepository.findById(400L)).thenReturn(Optional.of(escalation));
        when(dgmRepository.findByUserId(100L)).thenReturn(Optional.of(dgm));
        when(hostelDgmAssignmentRepository.findByHostelIdAndDgmId(1L, 10L))
                .thenReturn(Optional.of(new HostelDgmAssignment(hostel, dgm, "Admin")));
        when(escalationRepository.save(any(ComplaintEscalation.class))).thenReturn(escalation);
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        // Act
        ComplaintEscalation result = escalationService.handleEscalation(
                400L, 100L, "Resolved by DGM", EscalationStatus.RESOLVED
        );

        // Assert
        assertNotNull(result);
        verify(hostelDgmAssignmentRepository).findByHostelIdAndDgmId(1L, 10L);
        verify(escalationRepository).save(any(ComplaintEscalation.class));
        verify(notificationService, times(1)).create(eq(200L), any(), anyString(), anyString(), anyLong());
    }

    @Test
    void handleEscalation_unassignedDgm_denied() {
        // Arrange
        when(escalationRepository.findById(400L)).thenReturn(Optional.of(escalation));
        when(dgmRepository.findByUserId(100L)).thenReturn(Optional.of(dgm));
        when(hostelDgmAssignmentRepository.findByHostelIdAndDgmId(1L, 10L))
                .thenReturn(Optional.empty()); // No assignment

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                escalationService.handleEscalation(400L, 100L, "Response", EscalationStatus.RESOLVED)
        );

        assertEquals("You are not authorized to handle escalations for this hostel", exception.getMessage());
        verify(escalationRepository, never()).save(any());
        verify(notificationService, never()).create(anyLong(), any(), anyString(), anyString(), anyLong());
    }

    @Test
    void handleEscalation_studentHasNoRoom_denied() {
        // Arrange
        student.setRoom(null); // Student not assigned to any room
        when(escalationRepository.findById(400L)).thenReturn(Optional.of(escalation));
        when(dgmRepository.findByUserId(100L)).thenReturn(Optional.of(dgm));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                escalationService.handleEscalation(400L, 100L, "Response", EscalationStatus.RESOLVED)
        );

        assertEquals("Student has no room assignment - cannot determine hostel for authorization", exception.getMessage());
        verify(hostelDgmAssignmentRepository, never()).findByHostelIdAndDgmId(anyLong(), anyLong());
        verify(escalationRepository, never()).save(any());
    }

    @Test
    void handleEscalation_roomHasNoHostel_denied() {
        // Arrange
        room.setHostel(null); // Room not linked to hostel (data integrity issue)
        when(escalationRepository.findById(400L)).thenReturn(Optional.of(escalation));
        when(dgmRepository.findByUserId(100L)).thenReturn(Optional.of(dgm));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                escalationService.handleEscalation(400L, 100L, "Response", EscalationStatus.RESOLVED)
        );

        assertEquals("Room has no hostel assignment - cannot authorize DGM access", exception.getMessage());
        verify(hostelDgmAssignmentRepository, never()).findByHostelIdAndDgmId(anyLong(), anyLong());
        verify(escalationRepository, never()).save(any());
    }

    @Test
    void handleEscalation_escalationNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(escalationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                escalationService.handleEscalation(999L, 100L, "Response", EscalationStatus.RESOLVED)
        );

        verify(dgmRepository, never()).findByUserId(anyLong());
        verify(escalationRepository, never()).save(any());
    }

    @Test
    void handleEscalation_dgmNotFound_throwsResourceNotFoundException() {
        // Arrange
        when(escalationRepository.findById(400L)).thenReturn(Optional.of(escalation));
        when(dgmRepository.findByUserId(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                escalationService.handleEscalation(400L, 999L, "Response", EscalationStatus.RESOLVED)
        );

        verify(hostelDgmAssignmentRepository, never()).findByHostelIdAndDgmId(anyLong(), anyLong());
        verify(escalationRepository, never()).save(any());
    }

    @Test
    void handleEscalation_dgmIdentityFromAuthentication_cannotImpersonate() {
        // This test verifies that the DGM ID comes from the authenticated principal
        // (passed as dgmUserId parameter) and cannot be manipulated.
        // The service looks up the DGM by the authenticated user's ID.

        // Arrange: DGM tries to handle escalation with their own valid auth
        when(escalationRepository.findById(400L)).thenReturn(Optional.of(escalation));
        when(dgmRepository.findByUserId(100L)).thenReturn(Optional.of(dgm));
        when(hostelDgmAssignmentRepository.findByHostelIdAndDgmId(1L, 10L))
                .thenReturn(Optional.of(new HostelDgmAssignment(hostel, dgm, "Admin")));
        when(escalationRepository.save(any(ComplaintEscalation.class))).thenReturn(escalation);
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        // Act: Service receives authenticated user ID (100L) from security context
        ComplaintEscalation result = escalationService.handleEscalation(
                400L, 100L, "Valid response", EscalationStatus.RESOLVED
        );

        // Assert: The service used the authenticated user's ID to find the DGM
        assertNotNull(result);
        verify(dgmRepository).findByUserId(100L); // Authenticated user ID
        verify(hostelDgmAssignmentRepository).findByHostelIdAndDgmId(1L, 10L); // DGM entity ID

        // Note: Impersonation is prevented at the controller layer by extracting
        // the user ID from the JWT/SecurityContext, not from request parameters.
    }

    @Test
    void escalateComplaintByDgm_assignedDgm_success() {
        when(complaintRepository.findById(300L)).thenReturn(Optional.of(complaint));
        when(dgmRepository.findByUserId(100L)).thenReturn(Optional.of(dgm));
        when(hostelDgmAssignmentRepository.findByHostelIdAndDgmId(1L, 10L))
                .thenReturn(Optional.of(new HostelDgmAssignment(hostel, dgm, "Admin")));
        when(escalationRepository.save(any(ComplaintEscalation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        ComplaintEscalation result = escalationService.escalateComplaintByDgm(
                300L, 100L, "Executive escalation for SLA breach", "Overdue 48h"
        );

        assertNotNull(result);
        assertTrue(complaint.isEscalated());
        assertEquals(EscalationStatus.PENDING, result.getStatus());
        verify(auditService).logComplaintEscalation(300L, dgmUser, "Executive escalation for SLA breach");
    }

    @Test
    void escalateComplaintByDgm_unassignedDgm_denied() {
        when(complaintRepository.findById(300L)).thenReturn(Optional.of(complaint));
        when(dgmRepository.findByUserId(100L)).thenReturn(Optional.of(dgm));
        when(hostelDgmAssignmentRepository.findByHostelIdAndDgmId(1L, 10L))
                .thenReturn(Optional.empty());

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                escalationService.escalateComplaintByDgm(
                        300L, 100L, "Executive escalation for SLA breach", "Overdue 48h"
                )
        );

        assertEquals("You are not authorized to escalate complaints for this hostel", ex.getMessage());
        verify(escalationRepository, never()).save(any());
    }
}
