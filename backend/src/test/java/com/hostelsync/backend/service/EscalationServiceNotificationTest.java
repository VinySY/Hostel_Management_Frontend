package com.hostelsync.backend.service;

import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for escalation notification scoping in EscalationService.
 * Verifies only DGMs assigned to the relevant hostel are notified.
 */
@ExtendWith(MockitoExtension.class)
class EscalationServiceNotificationTest {

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

    private Hostel northHostel;
    private Hostel southHostel;
    private Room northRoom;
    private User studentUser;
    private Student student;
    private Complaint complaint;
    private Dgm dgm1;
    private Dgm dgm2;
    private Dgm dgm3;
    private HostelDgmAssignment assignment1;
    private HostelDgmAssignment assignment2;

    @BeforeEach
    void setUp() {
        // Setup hostels
        northHostel = new Hostel("North Hostel", "North Campus");
        northHostel.setId(1L);

        southHostel = new Hostel("South Hostel", "South Campus");
        southHostel.setId(2L);

        // Setup room in North Hostel
        northRoom = new Room("101", "Block A", 1, 2, northHostel);
        northRoom.setId(50L);

        // Setup student in North Hostel
        studentUser = new User("Student User", "student@test.com", "hashedpw", UserRole.STUDENT);
        studentUser.setId(200L);

        student = new Student();
        student.setId(20L);
        student.setUser(studentUser);
        student.setStudentId("STU-2024-001");
        student.setRoom(northRoom);
        student.setCourse("Computer Science");
        student.setYear(2);
        student.setPhone("1234567890");

        // Setup complaint
        complaint = new Complaint();
        complaint.setId(300L);
        complaint.setComplaintNumber("CMP-300");
        complaint.setStudent(student);
        complaint.setType(ComplaintType.Electricity);
        complaint.setSubject("Power outage");
        complaint.setDescription("Room has no power for 3 hours");
        complaint.setLocation("Room 101");
        complaint.setPriority(ComplaintPriority.High);
        complaint.setStatus(ComplaintStatus.Overdue);
        complaint.setSlaDeadline(LocalDateTime.now().minusHours(2));

        // Setup DGMs
        User dgmUser1 = new User("DGM North 1", "dgm1@test.com", "hashedpw", UserRole.DGM);
        dgmUser1.setId(101L);
        dgm1 = new Dgm();
        dgm1.setId(11L);
        dgm1.setUser(dgmUser1);
        dgm1.setEmployeeId("DGM-001");

        User dgmUser2 = new User("DGM North 2", "dgm2@test.com", "hashedpw", UserRole.DGM);
        dgmUser2.setId(102L);
        dgm2 = new Dgm();
        dgm2.setId(12L);
        dgm2.setUser(dgmUser2);
        dgm2.setEmployeeId("DGM-002");

        User dgmUser3 = new User("DGM South", "dgm3@test.com", "hashedpw", UserRole.DGM);
        dgmUser3.setId(103L);
        dgm3 = new Dgm();
        dgm3.setId(13L);
        dgm3.setUser(dgmUser3);
        dgm3.setEmployeeId("DGM-003");

        // Setup assignments: dgm1 and dgm2 assigned to North, dgm3 to South
        assignment1 = new HostelDgmAssignment(northHostel, dgm1, "Admin");
        assignment2 = new HostelDgmAssignment(northHostel, dgm2, "Admin");
    }

    @Test
    void escalateComplaint_notifiesOnlyAssignedDgms() {
        // Arrange: Complaint in North Hostel, only DGMs 1 and 2 should be notified
        when(complaintRepository.findById(300L)).thenReturn(Optional.of(complaint));
        when(slaService.isOverdue(any())).thenReturn(true);
        when(hostelDgmAssignmentRepository.findByHostelId(1L))
                .thenReturn(List.of(assignment1, assignment2)); // Only North Hostel DGMs
        when(escalationRepository.save(any(ComplaintEscalation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        // Act
        escalationService.escalateComplaint(300L, 200L, "Unresolved for too long", "Details");

        // Assert: Only DGMs assigned to North Hostel are notified
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(notificationService, times(2)).notifyComplaintEscalated(
                userIdCaptor.capture(), eq(300L), eq("CMP-300")
        );

        List<Long> notifiedUserIds = userIdCaptor.getAllValues();
        assertEquals(2, notifiedUserIds.size());
        assertTrue(notifiedUserIds.contains(101L)); // DGM1
        assertTrue(notifiedUserIds.contains(102L)); // DGM2
        assertFalse(notifiedUserIds.contains(103L)); // DGM3 (South) not notified
    }

    @Test
    void escalateComplaint_studentNoRoom_noNotifications() {
        // Arrange: Student has no room assignment
        student.setRoom(null);
        when(complaintRepository.findById(300L)).thenReturn(Optional.of(complaint));
        when(slaService.isOverdue(any())).thenReturn(true);
        when(escalationRepository.save(any(ComplaintEscalation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        // Act
        escalationService.escalateComplaint(300L, 200L, "Unresolved", "Details");

        // Assert: No notifications sent
        verify(hostelDgmAssignmentRepository, never()).findByHostelId(anyLong());
        verify(notificationService, never()).notifyComplaintEscalated(anyLong(), anyLong(), anyString());
    }

    @Test
    void escalateComplaint_roomNoHostel_noNotifications() {
        // Arrange: Room has no hostel assignment (data integrity issue)
        northRoom.setHostel(null);
        when(complaintRepository.findById(300L)).thenReturn(Optional.of(complaint));
        when(slaService.isOverdue(any())).thenReturn(true);
        when(escalationRepository.save(any(ComplaintEscalation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        // Act
        escalationService.escalateComplaint(300L, 200L, "Unresolved", "Details");

        // Assert: No notifications sent
        verify(hostelDgmAssignmentRepository, never()).findByHostelId(anyLong());
        verify(notificationService, never()).notifyComplaintEscalated(anyLong(), anyLong(), anyString());
    }

    @Test
    void escalateComplaint_noDgmsAssigned_noNotifications() {
        // Arrange: No DGMs assigned to the hostel
        when(complaintRepository.findById(300L)).thenReturn(Optional.of(complaint));
        when(slaService.isOverdue(any())).thenReturn(true);
        when(hostelDgmAssignmentRepository.findByHostelId(1L))
                .thenReturn(List.of()); // Empty list
        when(escalationRepository.save(any(ComplaintEscalation.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        // Act
        escalationService.escalateComplaint(300L, 200L, "Unresolved", "Details");

        // Assert: Escalation created but no notifications sent
        verify(hostelDgmAssignmentRepository).findByHostelId(1L);
        verify(notificationService, never()).notifyComplaintEscalated(anyLong(), anyLong(), anyString());
        verify(escalationRepository).save(any(ComplaintEscalation.class));
    }

    @Test
    void escalateComplaint_notOverdue_throwsBadRequest() {
        // Arrange: Complaint is not overdue
        when(complaintRepository.findById(300L)).thenReturn(Optional.of(complaint));
        when(slaService.isOverdue(any())).thenReturn(false);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                escalationService.escalateComplaint(300L, 200L, "Premature escalation", "Details")
        );

        assertEquals("Complaint can only be escalated if it is overdue", exception.getMessage());
        verify(escalationRepository, never()).save(any());
        verify(notificationService, never()).notifyComplaintEscalated(anyLong(), anyLong(), anyString());
    }

    @Test
    void escalateComplaint_alreadyEscalated_throwsBadRequest() {
        // Arrange: Complaint already escalated
        complaint.setEscalated(true);
        when(complaintRepository.findById(300L)).thenReturn(Optional.of(complaint));
        when(slaService.isOverdue(any())).thenReturn(true);

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                escalationService.escalateComplaint(300L, 200L, "Already escalated", "Details")
        );

        assertEquals("Complaint is already escalated", exception.getMessage());
        verify(escalationRepository, never()).save(any());
        verify(notificationService, never()).notifyComplaintEscalated(anyLong(), anyLong(), anyString());
    }
}
