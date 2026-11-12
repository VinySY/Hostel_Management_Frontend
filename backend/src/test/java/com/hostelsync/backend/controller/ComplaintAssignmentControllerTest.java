package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.ComplaintAssignmentRequest;
import com.hostelsync.backend.dto.ComplaintAssignmentResponse;
import com.hostelsync.backend.entity.Complaint;
import com.hostelsync.backend.entity.ComplaintAssignment;
import com.hostelsync.backend.entity.User;
import com.hostelsync.backend.entity.UserRole;
import com.hostelsync.backend.entity.Warden;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.service.ComplaintAssignmentService;
import com.hostelsync.backend.service.ComplaintService;
import com.hostelsync.backend.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintAssignmentControllerTest {

    @Mock
    private ComplaintService complaintService;

    @Mock
    private StudentService studentService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ComplaintAssignmentService complaintAssignmentService;

    @InjectMocks
    private ComplaintController complaintController;

    private User assignerUser;
    private User wardenUser;
    private Warden warden;
    private Complaint complaint;
    private ComplaintAssignment assignment;
    private UserDetails wardenPrincipal;

    @BeforeEach
    void setUp() {
        assignerUser = new User();
        assignerUser.setId(50L);
        assignerUser.setName("Chief Warden Alex");
        assignerUser.setEmail("chief@hostelsync.com");
        assignerUser.setRole(UserRole.WARDEN);

        wardenUser = new User();
        wardenUser.setId(60L);
        wardenUser.setName("Floor Warden Bob");
        wardenUser.setEmail("bob@hostelsync.com");
        wardenUser.setRole(UserRole.WARDEN);

        warden = new Warden();
        warden.setId(5L);
        warden.setUser(wardenUser);

        complaint = new Complaint();
        complaint.setId(100L);
        complaint.setComplaintNumber("CMP-100");

        assignment = new ComplaintAssignment();
        assignment.setId(1L);
        assignment.setComplaint(complaint);
        assignment.setAssignedWarden(warden);
        assignment.setAssignedById(50L);
        assignment.setReason("Urgent plumbing repair");
        assignment.setAssignedAt(LocalDateTime.now());

        wardenPrincipal = org.springframework.security.core.userdetails.User
                .withUsername("chief@hostelsync.com").password("pass").roles("WARDEN").build();
    }

    @Test
    void getAssignmentHistory_resolvesAssignedByName_withoutNPlusOne() {
        when(complaintService.findEntityById(100L)).thenReturn(complaint);
        when(complaintAssignmentService.getAssignmentHistory(100L)).thenReturn(List.of(assignment));
        when(userRepository.findAllById(Set.of(50L))).thenReturn(List.of(assignerUser));

        ResponseEntity<List<ComplaintAssignmentResponse>> response = complaintController.getAssignmentHistory(100L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        ComplaintAssignmentResponse item = response.getBody().get(0);
        assertEquals(1L, item.getId());
        assertEquals(100L, item.getComplaintId());
        assertEquals("CMP-100", item.getComplaintNumber());
        assertEquals(5L, item.getAssignedWardenId());
        assertEquals("Floor Warden Bob", item.getAssignedWardenName());
        assertEquals(50L, item.getAssignedById());
        assertEquals("Chief Warden Alex", item.getAssignedByName());
        assertEquals("Urgent plumbing repair", item.getNotes());

        verify(userRepository, times(1)).findAllById(Set.of(50L));
    }

    @Test
    void getAssignmentHistory_userNotFound_fallsBackToStaff() {
        when(complaintService.findEntityById(100L)).thenReturn(complaint);
        when(complaintAssignmentService.getAssignmentHistory(100L)).thenReturn(List.of(assignment));
        when(userRepository.findAllById(Set.of(50L))).thenReturn(List.of());

        ResponseEntity<List<ComplaintAssignmentResponse>> response = complaintController.getAssignmentHistory(100L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Staff", response.getBody().get(0).getAssignedByName());
    }

    @Test
    void assignComplaint_success() {
        when(userRepository.findByEmail("chief@hostelsync.com")).thenReturn(Optional.of(assignerUser));
        when(complaintAssignmentService.assignComplaint(100L, 5L, 50L, "Assigning task"))
                .thenReturn(assignment);

        ComplaintAssignmentRequest request = new ComplaintAssignmentRequest(5L, "Assigning task");

        ResponseEntity<ComplaintAssignmentResponse> response =
                complaintController.assignComplaint(100L, request, wardenPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Chief Warden Alex", response.getBody().getAssignedByName());
        assertEquals("Floor Warden Bob", response.getBody().getAssignedWardenName());
    }
}
