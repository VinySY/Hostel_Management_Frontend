package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.DgmDashboardResponse;
import com.hostelsync.backend.dto.EscalationRequest;
import com.hostelsync.backend.dto.EscalationResponse;
import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.repository.DgmRepository;
import com.hostelsync.backend.repository.HostelRepository;
import com.hostelsync.backend.repository.UserRepository;
import com.hostelsync.backend.service.DgmService;
import com.hostelsync.backend.service.EscalationService;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DgmControllerTest {

    @Mock
    private DgmService dgmService;

    @Mock
    private EscalationService escalationService;

    @Mock
    private HostelRepository hostelRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DgmRepository dgmRepository;

    @InjectMocks
    private DgmController dgmController;

    private User dgmUser;
    private Dgm dgm;
    private UserDetails dgmPrincipal;

    @BeforeEach
    void setUp() {
        dgmUser = new User();
        dgmUser.setId(77L);
        dgmUser.setEmail("dgm@hostelsync.com");
        dgmUser.setName("DGM Director");
        dgmUser.setRole(UserRole.DGM);

        dgm = new Dgm();
        dgm.setId(10L);
        dgm.setUser(dgmUser);

        dgmPrincipal = org.springframework.security.core.userdetails.User
                .withUsername("dgm@hostelsync.com").password("pass").roles("DGM").build();
    }

    @Test
    void getDashboard_returnsComputedMetrics() {
        when(userRepository.findByEmail("dgm@hostelsync.com")).thenReturn(Optional.of(dgmUser));

        DgmDashboardResponse dashboardResponse = new DgmDashboardResponse(
                25, // totalComplaints
                4, // pendingEscalations
                12, // resolvedEscalations
                5, // pendingAssignments
                Map.of("Hostel A", 15, "Hostel B", 10),
                Map.of("PENDING", 4, "RESOLVED", 12),
                List.of(new DgmDashboardResponse.DashboardHostelStats(1L, "Hostel A", 15, 2, 4, 12, 0))
        );

        when(dgmService.getDashboardResponse(77L)).thenReturn(dashboardResponse);

        ResponseEntity<DgmDashboardResponse> response = dgmController.getDashboard(dgmPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(25, response.getBody().getTotalComplaints());
        assertEquals(4, response.getBody().getPendingEscalations());
        assertEquals(12, response.getBody().getResolvedEscalations());
        assertEquals(5, response.getBody().getPendingAssignments());
    }

    @Test
    void getAssignedHostels_returnsHostelList() {
        when(userRepository.findByEmail("dgm@hostelsync.com")).thenReturn(Optional.of(dgmUser));
        when(dgmService.getDgmByUserId(77L)).thenReturn(dgm);

        Hostel h1 = new Hostel();
        h1.setId(1L);
        h1.setName("Hostel Alpha");

        when(dgmService.getHostelsForDgm(10L)).thenReturn(List.of(h1));

        ResponseEntity<List<Hostel>> response = dgmController.getAssignedHostels(dgmPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Hostel Alpha", response.getBody().get(0).getName());
    }

    @Test
    void handleEscalation_success() {
        when(userRepository.findByEmail("dgm@hostelsync.com")).thenReturn(Optional.of(dgmUser));
        when(dgmRepository.findById(10L)).thenReturn(Optional.of(dgm));

        Complaint complaint = new Complaint();
        complaint.setId(101L);
        complaint.setComplaintNumber("CMP-101");

        ComplaintEscalation handled = new ComplaintEscalation();
        handled.setId(5L);
        handled.setComplaint(complaint);
        handled.setHandledByDgmId(10L);
        handled.setDgmResponse("Resolved after inspection");
        handled.setStatus(EscalationStatus.RESOLVED);
        handled.setHandledAt(LocalDateTime.now());
        handled.setEscalatedAt(LocalDateTime.now());

        when(escalationService.handleEscalation(eq(5L), eq(77L), eq("Resolved after inspection"), eq(EscalationStatus.RESOLVED)))
                .thenReturn(handled);

        Map<String, String> request = Map.of(
                "status", "RESOLVED",
                "response", "Resolved after inspection"
        );

        ResponseEntity<EscalationResponse> response = dgmController.handleEscalation(5L, request, dgmPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(EscalationStatus.RESOLVED, response.getBody().getStatus());
        assertEquals("DGM Director", response.getBody().getHandledByDgmName());
    }

    @Test
    void handleEscalation_withDismissedAlias_mapsToRejected() {
        when(userRepository.findByEmail("dgm@hostelsync.com")).thenReturn(Optional.of(dgmUser));
        when(dgmRepository.findById(10L)).thenReturn(Optional.of(dgm));

        Complaint complaint = new Complaint();
        complaint.setId(101L);
        complaint.setComplaintNumber("CMP-101");

        ComplaintEscalation handled = new ComplaintEscalation();
        handled.setId(5L);
        handled.setComplaint(complaint);
        handled.setHandledByDgmId(10L);
        handled.setDgmResponse("Dismissed");
        handled.setStatus(EscalationStatus.REJECTED);
        handled.setHandledAt(LocalDateTime.now());
        handled.setEscalatedAt(LocalDateTime.now());

        when(escalationService.handleEscalation(eq(5L), eq(77L), eq("Dismissed"), eq(EscalationStatus.REJECTED)))
                .thenReturn(handled);

        Map<String, String> request = Map.of(
                "status", "DISMISSED",
                "response", "Dismissed"
        );

        ResponseEntity<EscalationResponse> response = dgmController.handleEscalation(5L, request, dgmPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(EscalationStatus.REJECTED, response.getBody().getStatus());
    }

    @Test
    void escalateComplaintAsDgm_returnsCreatedEscalation() {
        when(userRepository.findByEmail("dgm@hostelsync.com")).thenReturn(Optional.of(dgmUser));

        Complaint complaint = new Complaint();
        complaint.setId(101L);
        complaint.setComplaintNumber("CMP-101");

        ComplaintEscalation created = new ComplaintEscalation();
        created.setId(9L);
        created.setComplaint(complaint);
        created.setReason("Direct DGM executive intervention due to SLA breach");
        created.setStatus(EscalationStatus.PENDING);
        created.setEscalatedAt(LocalDateTime.now());

        when(escalationService.escalateComplaintByDgm(
                eq(101L),
                eq(77L),
                eq("Direct DGM executive intervention due to SLA breach"),
                eq("Initiated from overdue dashboard.")
        )).thenReturn(created);

        EscalationRequest request = new EscalationRequest(
                "Direct DGM executive intervention due to SLA breach",
                "Initiated from overdue dashboard."
        );

        ResponseEntity<EscalationResponse> response =
                dgmController.escalateComplaintAsDgm(101L, request, dgmPrincipal);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(101L, response.getBody().getComplaintId());
        assertEquals(EscalationStatus.PENDING, response.getBody().getStatus());
    }
}
