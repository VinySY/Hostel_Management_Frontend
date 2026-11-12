package com.hostelsync.backend.service;

import com.hostelsync.backend.dto.ComplaintResponse;
import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.exception.ForbiddenException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.ComplaintAttachmentRepository;
import com.hostelsync.backend.repository.ComplaintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for complaint reopening workflow in ComplaintService.
 * Verifies authorization, valid status transitions, and audit history.
 */
@ExtendWith(MockitoExtension.class)
class ComplaintReopeningTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private ComplaintAttachmentRepository attachmentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MappingService mappingService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    private ComplaintService complaintService;

    private User studentUser;
    private Student student;
    private User otherStudentUser;
    private Student otherStudent;
    private User wardenUser;
    private Warden warden;
    private Complaint resolvedComplaint;
    private Complaint rejectedComplaint;
    private Complaint submittedComplaint;
    private Complaint reopenedComplaint;

    @BeforeEach
    void setUp() {
        complaintService = new ComplaintService(
                complaintRepository,
                attachmentRepository,
                fileStorageService,
                mappingService,
                notificationService,
                auditService,
                "http://localhost:3000",
                8080
        );

        // Setup student
        studentUser = new User("Student User", "student@test.com", "hashedpw", UserRole.STUDENT);
        studentUser.setId(100L);

        student = new Student();
        student.setId(10L);
        student.setUser(studentUser);
        student.setStudentId("STU-2024-001");
        student.setCourse("Computer Science");
        student.setYear(2);
        student.setPhone("1234567890");

        // Setup another student
        otherStudentUser = new User("Other Student", "other@test.com", "hashedpw", UserRole.STUDENT);
        otherStudentUser.setId(101L);

        otherStudent = new Student();
        otherStudent.setId(11L);
        otherStudent.setUser(otherStudentUser);
        otherStudent.setStudentId("STU-2024-002");
        otherStudent.setCourse("Engineering");
        otherStudent.setYear(3);
        otherStudent.setPhone("9876543210");

        // Setup warden
        wardenUser = new User("Warden User", "warden@test.com", "hashedpw", UserRole.WARDEN);
        wardenUser.setId(200L);

        warden = new Warden();
        warden.setId(20L);
        warden.setUser(wardenUser);
        warden.setEmployeeId("WDN-001");

        // Setup resolved complaint
        resolvedComplaint = new Complaint();
        resolvedComplaint.setId(1000L);
        resolvedComplaint.setComplaintNumber("CMP-1000");
        resolvedComplaint.setStudent(student);
        resolvedComplaint.setType(ComplaintType.Electricity);
        resolvedComplaint.setSubject("Power issue resolved");
        resolvedComplaint.setDescription("The power issue has been resolved by maintenance team");
        resolvedComplaint.setLocation("Room 101");
        resolvedComplaint.setPriority(ComplaintPriority.High);
        resolvedComplaint.setStatus(ComplaintStatus.Resolved);
        resolvedComplaint.setResolvedAt(LocalDateTime.now().minusDays(1));
        resolvedComplaint.setAssignedWarden(warden);
        resolvedComplaint.setReopened(false);
        resolvedComplaint.setReopenCount(0);

        // Setup rejected complaint
        rejectedComplaint = new Complaint();
        rejectedComplaint.setId(1001L);
        rejectedComplaint.setComplaintNumber("CMP-1001");
        rejectedComplaint.setStudent(student);
        rejectedComplaint.setType(ComplaintType.Mess);
        rejectedComplaint.setSubject("Invalid complaint");
        rejectedComplaint.setDescription("This complaint was rejected due to insufficient evidence");
        rejectedComplaint.setLocation("Mess Hall");
        rejectedComplaint.setPriority(ComplaintPriority.Low);
        rejectedComplaint.setStatus(ComplaintStatus.Rejected);
        rejectedComplaint.setAssignedWarden(null);
        rejectedComplaint.setReopened(false);
        rejectedComplaint.setReopenCount(0);

        // Setup submitted complaint (active)
        submittedComplaint = new Complaint();
        submittedComplaint.setId(1002L);
        submittedComplaint.setComplaintNumber("CMP-1002");
        submittedComplaint.setStudent(student);
        submittedComplaint.setType(ComplaintType.Water);
        submittedComplaint.setSubject("Water supply issue");
        submittedComplaint.setDescription("No water supply in the hostel since morning today");
        submittedComplaint.setLocation("Room 102");
        submittedComplaint.setPriority(ComplaintPriority.Urgent);
        submittedComplaint.setStatus(ComplaintStatus.Submitted);
        submittedComplaint.setReopened(false);
        submittedComplaint.setReopenCount(0);

        // Setup already reopened complaint
        reopenedComplaint = new Complaint();
        reopenedComplaint.setId(1003L);
        reopenedComplaint.setComplaintNumber("CMP-1003");
        reopenedComplaint.setStudent(student);
        reopenedComplaint.setType(ComplaintType.Plumbing);
        reopenedComplaint.setSubject("Plumbing issue");
        reopenedComplaint.setDescription("Plumbing issue that was already reopened once before");
        reopenedComplaint.setLocation("Room 103");
        reopenedComplaint.setPriority(ComplaintPriority.Medium);
        reopenedComplaint.setStatus(ComplaintStatus.Under_Review);
        reopenedComplaint.setReopened(true);
        reopenedComplaint.setReopenedAt(LocalDateTime.now().minusDays(1));
        reopenedComplaint.setReopenCount(1);
    }

    @Test
    void reopenComplaint_resolvedComplaint_success() {
        // Arrange
        when(complaintRepository.findById(1000L)).thenReturn(Optional.of(resolvedComplaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mappingService.toComplaintResponse(any(Complaint.class), anyString()))
                .thenReturn(new ComplaintResponse());

        // Act
        ComplaintResponse response = complaintService.reopenComplaint(1000L, 100L, "Issue persists");

        // Assert
        assertNotNull(response);
        assertEquals(ComplaintStatus.Under_Review, resolvedComplaint.getStatus());
        assertTrue(resolvedComplaint.isReopened());
        assertNotNull(resolvedComplaint.getReopenedAt());
        assertEquals(1, resolvedComplaint.getReopenCount());

        verify(complaintRepository).save(resolvedComplaint);
        verify(auditService).logComplaintReopening(eq(1000L), eq(studentUser), eq("Issue persists"));
        verify(notificationService).notifyComplaintReopened(eq(200L), eq(1000L), eq("CMP-1000"));
    }

    @Test
    void reopenComplaint_rejectedComplaint_success() {
        // Arrange
        when(complaintRepository.findById(1001L)).thenReturn(Optional.of(rejectedComplaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mappingService.toComplaintResponse(any(Complaint.class), anyString()))
                .thenReturn(new ComplaintResponse());

        // Act
        ComplaintResponse response = complaintService.reopenComplaint(1001L, 100L, "I have new evidence");

        // Assert
        assertNotNull(response);
        assertEquals(ComplaintStatus.Under_Review, rejectedComplaint.getStatus());
        assertTrue(rejectedComplaint.isReopened());
        assertNotNull(rejectedComplaint.getReopenedAt());
        assertEquals(1, rejectedComplaint.getReopenCount());

        verify(complaintRepository).save(rejectedComplaint);
        verify(auditService).logComplaintReopening(eq(1001L), eq(studentUser), eq("I have new evidence"));
        verify(notificationService, never()).notifyComplaintReopened(anyLong(), anyLong(), anyString());
    }

    @Test
    void reopenComplaint_withoutReason_success() {
        // Arrange
        when(complaintRepository.findById(1000L)).thenReturn(Optional.of(resolvedComplaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mappingService.toComplaintResponse(any(Complaint.class), anyString()))
                .thenReturn(new ComplaintResponse());

        // Act
        ComplaintResponse response = complaintService.reopenComplaint(1000L, 100L, null);

        // Assert
        assertNotNull(response);
        verify(auditService).logComplaintReopening(eq(1000L), eq(studentUser), eq("No reason provided"));
    }

    @Test
    void reopenComplaint_notOwnComplaint_forbidden() {
        // Arrange
        when(complaintRepository.findById(1000L)).thenReturn(Optional.of(resolvedComplaint));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                complaintService.reopenComplaint(1000L, 101L, "Trying to reopen")
        );

        assertEquals("You may only reopen your own complaints", exception.getMessage());
        verify(complaintRepository, never()).save(any());
        verify(auditService, never()).logComplaintReopening(anyLong(), any(), anyString());
        verify(notificationService, never()).notifyComplaintReopened(anyLong(), anyLong(), anyString());
    }

    @Test
    void reopenComplaint_submittedStatus_badRequest() {
        // Arrange
        when(complaintRepository.findById(1002L)).thenReturn(Optional.of(submittedComplaint));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                complaintService.reopenComplaint(1002L, 100L, "Trying to reopen active complaint")
        );

        assertEquals("Complaint can only be reopened if it is Resolved or Rejected", exception.getMessage());
        verify(complaintRepository, never()).save(any());
    }

    @Test
    void reopenComplaint_underReviewStatus_badRequest() {
        // Arrange
        Complaint underReviewComplaint = new Complaint();
        underReviewComplaint.setId(1004L);
        underReviewComplaint.setStudent(student);
        underReviewComplaint.setStatus(ComplaintStatus.Under_Review);
        underReviewComplaint.setReopened(false);

        when(complaintRepository.findById(1004L)).thenReturn(Optional.of(underReviewComplaint));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                complaintService.reopenComplaint(1004L, 100L, "Trying to reopen")
        );

        assertEquals("Complaint can only be reopened if it is Resolved or Rejected", exception.getMessage());
    }

    @Test
    void reopenComplaint_inProgressStatus_badRequest() {
        // Arrange
        Complaint inProgressComplaint = new Complaint();
        inProgressComplaint.setId(1005L);
        inProgressComplaint.setStudent(student);
        inProgressComplaint.setStatus(ComplaintStatus.In_Progress);
        inProgressComplaint.setReopened(false);

        when(complaintRepository.findById(1005L)).thenReturn(Optional.of(inProgressComplaint));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                complaintService.reopenComplaint(1005L, 100L, "Trying to reopen")
        );

        assertEquals("Complaint can only be reopened if it is Resolved or Rejected", exception.getMessage());
    }

    @Test
    void reopenComplaint_overdueStatus_badRequest() {
        // Arrange
        Complaint overdueComplaint = new Complaint();
        overdueComplaint.setId(1006L);
        overdueComplaint.setStudent(student);
        overdueComplaint.setStatus(ComplaintStatus.Overdue);
        overdueComplaint.setReopened(false);

        when(complaintRepository.findById(1006L)).thenReturn(Optional.of(overdueComplaint));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                complaintService.reopenComplaint(1006L, 100L, "Trying to reopen")
        );

        assertEquals("Complaint can only be reopened if it is Resolved or Rejected", exception.getMessage());
    }

    @Test
    void reopenComplaint_alreadyReopened_badRequest() {
        // Arrange
        when(complaintRepository.findById(1003L)).thenReturn(Optional.of(reopenedComplaint));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                complaintService.reopenComplaint(1003L, 100L, "Trying to reopen again")
        );

        assertEquals("Complaint is already reopened", exception.getMessage());
        verify(complaintRepository, never()).save(any());
        verify(auditService, never()).logComplaintReopening(anyLong(), any(), anyString());
    }

    @Test
    void reopenComplaint_notFound_throwsResourceNotFoundException() {
        // Arrange
        when(complaintRepository.findById(9999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                complaintService.reopenComplaint(9999L, 100L, "Trying to reopen")
        );

        verify(complaintRepository, never()).save(any());
    }

    @Test
    void reopenComplaint_timelineEntryCreated() {
        // Arrange
        when(complaintRepository.findById(1000L)).thenReturn(Optional.of(resolvedComplaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mappingService.toComplaintResponse(any(Complaint.class), anyString()))
                .thenReturn(new ComplaintResponse());

        // Act
        complaintService.reopenComplaint(1000L, 100L, "Issue persists after resolution");

        // Assert
        assertEquals(1, resolvedComplaint.getTimeline().size());
        ComplaintTimeline timelineEntry = resolvedComplaint.getTimeline().get(0);
        assertEquals(ComplaintStatus.Under_Review, timelineEntry.getStatus());
        assertEquals("Reopened by student: Issue persists after resolution", timelineEntry.getNote());
    }

    @Test
    void reopenComplaint_reopenCountIncremented() {
        // Arrange
        when(complaintRepository.findById(1000L)).thenReturn(Optional.of(resolvedComplaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mappingService.toComplaintResponse(any(Complaint.class), anyString()))
                .thenReturn(new ComplaintResponse());

        // Act
        complaintService.reopenComplaint(1000L, 100L, "Need to reopen");

        // Assert
        assertEquals(1, resolvedComplaint.getReopenCount());
    }

    @Test
    void reopenComplaint_noWardenAssigned_noNotification() {
        // Arrange: Rejected complaint has no warden assigned
        when(complaintRepository.findById(1001L)).thenReturn(Optional.of(rejectedComplaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mappingService.toComplaintResponse(any(Complaint.class), anyString()))
                .thenReturn(new ComplaintResponse());

        // Act
        complaintService.reopenComplaint(1001L, 100L, "New evidence");

        // Assert
        verify(notificationService, never()).notifyComplaintReopened(anyLong(), anyLong(), anyString());
    }
}
