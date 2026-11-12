package com.hostelsync.backend.service;

import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.repository.ComplaintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for OverdueDetectionJob.
 * Verifies correct overdue marking and duplicate prevention.
 */
@ExtendWith(MockitoExtension.class)
class OverdueDetectionJobTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private SlaService slaService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    private com.hostelsync.backend.job.OverdueDetectionJob overdueDetectionJob;

    private User studentUser;
    private Student student;
    private User wardenUser;
    private Warden warden;
    private Complaint overdueComplaint;
    private Complaint resolvedComplaint;
    private Complaint activeComplaint;

    @BeforeEach
    void setUp() {
        overdueDetectionJob = new com.hostelsync.backend.job.OverdueDetectionJob(
                complaintRepository, slaService, notificationService, auditService
        );

        // Setup student
        studentUser = new User("Student User", "student@test.com", "hashedpw", UserRole.STUDENT);
        studentUser.setId(200L);

        student = new Student();
        student.setId(20L);
        student.setUser(studentUser);
        student.setStudentId("STU-2024-001");
        student.setCourse("Computer Science");
        student.setYear(2);
        student.setPhone("1234567890");

        // Setup warden
        wardenUser = new User("Warden User", "warden@test.com", "hashedpw", UserRole.WARDEN);
        wardenUser.setId(300L);

        warden = new Warden();
        warden.setId(30L);
        warden.setUser(wardenUser);
        warden.setEmployeeId("WDN-001");

        // Setup overdue complaint (deadline passed)
        overdueComplaint = new Complaint();
        overdueComplaint.setId(100L);
        overdueComplaint.setComplaintNumber("CMP-100");
        overdueComplaint.setStudent(student);
        overdueComplaint.setType(ComplaintType.Electricity);
        overdueComplaint.setSubject("Power issue");
        overdueComplaint.setDescription("Description here with minimum 15 characters");
        overdueComplaint.setLocation("Room 101");
        overdueComplaint.setPriority(ComplaintPriority.High);
        overdueComplaint.setStatus(ComplaintStatus.Under_Review);
        overdueComplaint.setSlaDeadline(LocalDateTime.now().minusHours(2)); // Overdue
        overdueComplaint.setAssignedWarden(warden);

        // Setup resolved complaint (should not be marked overdue)
        resolvedComplaint = new Complaint();
        resolvedComplaint.setId(101L);
        resolvedComplaint.setComplaintNumber("CMP-101");
        resolvedComplaint.setStudent(student);
        resolvedComplaint.setType(ComplaintType.Plumbing);
        resolvedComplaint.setSubject("Leak");
        resolvedComplaint.setDescription("Water leak description with minimum characters");
        resolvedComplaint.setLocation("Room 102");
        resolvedComplaint.setPriority(ComplaintPriority.Medium);
        resolvedComplaint.setStatus(ComplaintStatus.Resolved);
        resolvedComplaint.setSlaDeadline(LocalDateTime.now().minusHours(1));
        resolvedComplaint.setResolvedAt(LocalDateTime.now().minusMinutes(30));

        // Setup active complaint (not yet overdue)
        activeComplaint = new Complaint();
        activeComplaint.setId(102L);
        activeComplaint.setComplaintNumber("CMP-102");
        activeComplaint.setStudent(student);
        activeComplaint.setType(ComplaintType.Room_Maintenance);
        activeComplaint.setSubject("Broken chair");
        activeComplaint.setDescription("Chair needs replacement with minimum characters here");
        activeComplaint.setLocation("Room 103");
        activeComplaint.setPriority(ComplaintPriority.Low);
        activeComplaint.setStatus(ComplaintStatus.In_Progress);
        activeComplaint.setSlaDeadline(LocalDateTime.now().plusHours(5)); // Still within SLA
    }

    @Test
    void detectOverdueComplaints_marksOverdueComplaint() {
        // Arrange
        when(complaintRepository.findAll()).thenReturn(List.of(overdueComplaint, resolvedComplaint, activeComplaint));
        when(slaService.isOverdue(overdueComplaint.getSlaDeadline())).thenReturn(true);
        // Note: resolvedComplaint.getStatus() == Resolved, so it's filtered out before isOverdue() is called
        // activeComplaint status != Overdue, so isOverdue() is called with false result
        when(slaService.isOverdue(activeComplaint.getSlaDeadline())).thenReturn(false);
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        overdueDetectionJob.detectOverdueComplaints();

        // Assert: Only overdueComplaint is marked
        verify(complaintRepository).save(overdueComplaint);
        assertEquals(ComplaintStatus.Overdue, overdueComplaint.getStatus());
        assertNotNull(overdueComplaint.getOverdueAt());
        verify(notificationService).notifyComplaintOverdue(eq(200L), eq(100L), eq("CMP-100"));
        verify(notificationService).notifyComplaintOverdue(eq(300L), eq(100L), eq("CMP-100"));
        verify(auditService).logComplaintStatusChange(
                eq(100L), anyString(), eq("Overdue"), any(User.class), anyString()
        );
    }

    @Test
    void detectOverdueComplaints_skipsResolvedComplaints() {
        // Arrange
        when(complaintRepository.findAll()).thenReturn(List.of(resolvedComplaint));
        // No isOverdue() stub needed — resolved complaints are filtered by status
        // before isOverdue() is ever called in the stream pipeline.

        // Act
        overdueDetectionJob.detectOverdueComplaints();

        // Assert: No complaints marked overdue
        verify(complaintRepository, never()).save(any(Complaint.class));
        verify(notificationService, never()).notifyComplaintOverdue(anyLong(), anyLong(), anyString());
    }

    @Test
    void detectOverdueComplaints_skipsRejectedComplaints() {
        // Arrange
        Complaint rejectedComplaint = new Complaint();
        rejectedComplaint.setId(103L);
        rejectedComplaint.setComplaintNumber("CMP-103");
        rejectedComplaint.setStudent(student);
        rejectedComplaint.setType(ComplaintType.Other);
        rejectedComplaint.setSubject("Invalid request");
        rejectedComplaint.setDescription("Rejected complaint description with minimum characters");
        rejectedComplaint.setLocation("Room 104");
        rejectedComplaint.setPriority(ComplaintPriority.Low);
        rejectedComplaint.setStatus(ComplaintStatus.Rejected);
        rejectedComplaint.setSlaDeadline(LocalDateTime.now().minusHours(3));

        when(complaintRepository.findAll()).thenReturn(List.of(rejectedComplaint));
        // No isOverdue() stub needed — rejected complaints are filtered by status
        // before isOverdue() is ever called in the stream pipeline.

        // Act
        overdueDetectionJob.detectOverdueComplaints();

        // Assert: No complaints marked overdue
        verify(complaintRepository, never()).save(any(Complaint.class));
        verify(notificationService, never()).notifyComplaintOverdue(anyLong(), anyLong(), anyString());
    }

    @Test
    void detectOverdueComplaints_skipsAlreadyOverdue() {
        // Arrange: Complaint already marked as Overdue
        overdueComplaint.setStatus(ComplaintStatus.Overdue);
        overdueComplaint.setOverdueAt(LocalDateTime.now().minusHours(1));

        when(complaintRepository.findAll()).thenReturn(List.of(overdueComplaint));
        // No isOverdue() stub needed — already-overdue complaints are filtered by status
        // before isOverdue() is ever called in the stream pipeline.

        // Act
        overdueDetectionJob.detectOverdueComplaints();

        // Assert: No changes made, no duplicate notifications
        verify(complaintRepository, never()).save(any(Complaint.class));
        verify(notificationService, never()).notifyComplaintOverdue(anyLong(), anyLong(), anyString());
        verify(auditService, never()).logComplaintStatusChange(anyLong(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void detectOverdueComplaints_handlesComplaintWithoutWarden() {
        // Arrange: Complaint has no assigned warden
        overdueComplaint.setAssignedWarden(null);

        when(complaintRepository.findAll()).thenReturn(List.of(overdueComplaint));
        when(slaService.isOverdue(overdueComplaint.getSlaDeadline())).thenReturn(true);
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        overdueDetectionJob.detectOverdueComplaints();

        // Assert: Only student is notified
        verify(notificationService).notifyComplaintOverdue(eq(200L), eq(100L), eq("CMP-100"));
        verify(notificationService, times(1)).notifyComplaintOverdue(anyLong(), anyLong(), anyString());
    }

    @Test
    void detectOverdueComplaints_handlesMultipleOverdueComplaints() {
        // Arrange: Multiple complaints overdue
        Complaint overdue2 = new Complaint();
        overdue2.setId(104L);
        overdue2.setComplaintNumber("CMP-104");
        overdue2.setStudent(student);
        overdue2.setType(ComplaintType.Other);
        overdue2.setSubject("Multiple issue");
        overdue2.setDescription("Another overdue complaint with minimum characters");
        overdue2.setLocation("Room 105");
        overdue2.setPriority(ComplaintPriority.Urgent);
        overdue2.setStatus(ComplaintStatus.Submitted);
        overdue2.setSlaDeadline(LocalDateTime.now().minusMinutes(30));

        when(complaintRepository.findAll()).thenReturn(List.of(overdueComplaint, overdue2));
        when(slaService.isOverdue(any())).thenReturn(true);
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        overdueDetectionJob.detectOverdueComplaints();

        // Assert: Both marked overdue
        verify(complaintRepository, times(2)).save(any(Complaint.class));
        assertEquals(ComplaintStatus.Overdue, overdueComplaint.getStatus());
        assertEquals(ComplaintStatus.Overdue, overdue2.getStatus());
    }
}
