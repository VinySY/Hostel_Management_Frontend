package com.hostelsync.backend.service;

import com.hostelsync.backend.dto.DgmDashboardResponse;
import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class DgmService {

    private final DgmRepository dgmRepository;
    private final HostelDgmAssignmentRepository hostelDgmAssignmentRepository;
    private final HostelRepository hostelRepository;
    private final ComplaintRepository complaintRepository;
    private final ComplaintEscalationRepository escalationRepository;
    private DemoScopeService demoScopeService;

    public DgmService(DgmRepository dgmRepository,
                     HostelDgmAssignmentRepository hostelDgmAssignmentRepository,
                     HostelRepository hostelRepository,
                     ComplaintRepository complaintRepository,
                     ComplaintEscalationRepository escalationRepository) {
        this.dgmRepository = dgmRepository;
        this.hostelDgmAssignmentRepository = hostelDgmAssignmentRepository;
        this.hostelRepository = hostelRepository;
        this.complaintRepository = complaintRepository;
        this.escalationRepository = escalationRepository;
    }

    @Autowired(required = false)
    public void setDemoScopeService(DemoScopeService demoScopeService) {
        this.demoScopeService = demoScopeService;
    }

    /**
     * Create a DGM account.
     */
    public Dgm createDgm(User user, String employeeId, String designation,
                        String officeLocation, String contactExtension) {
        if (user.getRole() != UserRole.DGM) {
            throw new BadRequestException("User must have DGM role");
        }

        Dgm dgm = new Dgm();
        dgm.setUser(user);
        dgm.setEmployeeId(employeeId);
        dgm.setDesignation(designation);
        dgm.setOfficeLocation(officeLocation);
        dgm.setContactExtension(contactExtension);
        return dgmRepository.save(dgm);
    }

    /**
     * Assign a DGM to a hostel.
     */
    public HostelDgmAssignment assignDgmToHostel(Long dgmId, Long hostelId, String assignedByUsername) {
        Dgm dgm = dgmRepository.findById(dgmId)
                .orElseThrow(() -> new ResourceNotFoundException("DGM", dgmId));

        Hostel hostel = hostelRepository.findById(hostelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hostel", hostelId));

        if (demoScopeService != null) {
            demoScopeService.assertSameEnvironmentPair(
                    demoScopeService.isDgmInDemoEnvironment(dgm),
                    demoScopeService.isDemoHostel(hostel)
            );
            demoScopeService.assertDgmAccessible(dgm);
            demoScopeService.assertDemoHostelAccess(hostel.getId());
        }

        // Check if assignment already exists
        if (hostelDgmAssignmentRepository.findByHostelIdAndDgmId(hostelId, dgmId).isPresent()) {
            throw new BadRequestException("DGM is already assigned to this hostel");
        }

        HostelDgmAssignment assignment = new HostelDgmAssignment();
        assignment.setHostel(hostel);
        assignment.setDgm(dgm);
        assignment.setAssignedBy(assignedByUsername);
        return hostelDgmAssignmentRepository.save(assignment);
    }

    /**
     * Get DGM by user ID.
     */
    @Transactional(readOnly = true)
    public Dgm getDgmByUserId(Long userId) {
        return dgmRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("DGM user", userId));
    }

    /**
     * Get all DGMs.
     */
    @Transactional(readOnly = true)
    public List<Dgm> getAllDgms() {
        List<Dgm> list = dgmRepository.findAll();
        if (demoScopeService != null) {
            return list.stream().filter(demoScopeService::isDgmAccessible).toList();
        }
        return list;
    }

    /**
     * Get hostels assigned to a DGM.
     */
    @Transactional(readOnly = true)
    public List<Hostel> getHostelsForDgm(Long dgmId) {
        List<HostelDgmAssignment> assignments = hostelDgmAssignmentRepository.findByDgmId(dgmId);
        return assignments.stream()
                .map(HostelDgmAssignment::getHostel)
                .filter(h -> demoScopeService == null || demoScopeService.isHostelAccessible(h.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Get DGMs assigned to a hostel.
     */
    @Transactional(readOnly = true)
    public List<Dgm> getDgmsForHostel(Long hostelId) {
        if (demoScopeService != null) {
            demoScopeService.assertDemoHostelAccess(hostelId);
        }
        List<HostelDgmAssignment> assignments = hostelDgmAssignmentRepository.findByHostelId(hostelId);
        return assignments.stream()
                .map(HostelDgmAssignment::getDgm)
                .filter(d -> demoScopeService == null || demoScopeService.isDgmAccessible(d))
                .collect(Collectors.toList());
    }

    /**
     * Check if DGM has access to a hostel.
     */
    @Transactional(readOnly = true)
    public boolean hasAccessToHostel(Long dgmId, Long hostelId) {
        return hostelDgmAssignmentRepository.findByHostelIdAndDgmId(hostelId, dgmId).isPresent();
    }

    /**
     * Get dashboard response with comprehensive statistics for a DGM.
     */
    @Transactional(readOnly = true)
    public DgmDashboardResponse getDashboardResponse(Long dgmUserId) {
        Dgm dgm = getDgmByUserId(dgmUserId);
        List<Hostel> hostels = getHostelsForDgm(dgm.getId());

        if (hostels.isEmpty()) {
            return new DgmDashboardResponse(
                    0, 0, 0, 0,
                    new java.util.LinkedHashMap<>(),
                    createDefaultEscalationMap(),
                    new java.util.ArrayList<>()
            );
        }

        List<Long> hostelIds = hostels.stream().map(Hostel::getId).collect(Collectors.toList());

        List<Complaint> complaints = complaintRepository.findByHostelIds(hostelIds);
        List<ComplaintEscalation> escalations = escalationRepository.findByHostelIds(hostelIds);

        int totalComplaints = complaints.size();
        int pendingAssignments = (int) complaints.stream()
                .filter(c -> c.getAssignedWarden() == null
                        && c.getStatus() != ComplaintStatus.Resolved
                        && c.getStatus() != ComplaintStatus.Rejected)
                .count();

        int pendingEscalations = (int) escalations.stream()
                .filter(e -> e.getStatus() == EscalationStatus.PENDING || e.getStatus() == EscalationStatus.IN_REVIEW)
                .count();

        int resolvedEscalations = (int) escalations.stream()
                .filter(e -> e.getStatus() == EscalationStatus.RESOLVED)
                .count();

        // Map complaints by hostel
        Map<String, Integer> complaintsByHostel = new java.util.LinkedHashMap<>();
        for (Hostel h : hostels) {
            complaintsByHostel.put(h.getName(), 0);
        }
        for (Complaint c : complaints) {
            if (c.getStudent() != null && c.getStudent().getRoom() != null && c.getStudent().getRoom().getHostel() != null) {
                String hName = c.getStudent().getRoom().getHostel().getName();
                complaintsByHostel.put(hName, complaintsByHostel.getOrDefault(hName, 0) + 1);
            }
        }

        // Map escalation status counts
        Map<String, Integer> escalationStatus = new java.util.LinkedHashMap<>();
        escalationStatus.put("PENDING", (int) escalations.stream().filter(e -> e.getStatus() == EscalationStatus.PENDING).count());
        escalationStatus.put("IN_REVIEW", (int) escalations.stream().filter(e -> e.getStatus() == EscalationStatus.IN_REVIEW).count());
        escalationStatus.put("RESOLVED", (int) escalations.stream().filter(e -> e.getStatus() == EscalationStatus.RESOLVED).count());
        escalationStatus.put("REJECTED", (int) escalations.stream().filter(e -> e.getStatus() == EscalationStatus.REJECTED).count());

        // Per-hostel statistics
        List<DgmDashboardResponse.DashboardHostelStats> hostelStats = new java.util.ArrayList<>();
        for (Hostel h : hostels) {
            List<Complaint> hComplaints = complaints.stream()
                    .filter(c -> c.getStudent() != null
                            && c.getStudent().getRoom() != null
                            && c.getStudent().getRoom().getHostel() != null
                            && c.getStudent().getRoom().getHostel().getId().equals(h.getId()))
                    .collect(Collectors.toList());

            List<ComplaintEscalation> hEscalations = escalations.stream()
                    .filter(e -> e.getComplaint() != null
                            && e.getComplaint().getStudent() != null
                            && e.getComplaint().getStudent().getRoom() != null
                            && e.getComplaint().getStudent().getRoom().getHostel() != null
                            && e.getComplaint().getStudent().getRoom().getHostel().getId().equals(h.getId()))
                    .collect(Collectors.toList());

            int hTotal = hComplaints.size();
            int hOverdue = (int) hComplaints.stream().filter(c -> c.getStatus() == ComplaintStatus.Overdue || c.getOverdueAt() != null).count();
            int hPendingEscalations = (int) hEscalations.stream().filter(e -> e.getStatus() == EscalationStatus.PENDING || e.getStatus() == EscalationStatus.IN_REVIEW).count();
            int hResolved = (int) hComplaints.stream().filter(c -> c.getStatus() == ComplaintStatus.Resolved).count();
            int hReopened = (int) hComplaints.stream().filter(c -> c.isReopened() || c.getReopenCount() > 0).count();

            hostelStats.add(new DgmDashboardResponse.DashboardHostelStats(
                    h.getId(),
                    h.getName(),
                    hTotal,
                    hOverdue,
                    hPendingEscalations,
                    hResolved,
                    hReopened
            ));
        }

        return new DgmDashboardResponse(
                totalComplaints,
                pendingEscalations,
                resolvedEscalations,
                pendingAssignments,
                complaintsByHostel,
                escalationStatus,
                hostelStats
        );
    }

    private Map<String, Integer> createDefaultEscalationMap() {
        Map<String, Integer> map = new java.util.LinkedHashMap<>();
        map.put("PENDING", 0);
        map.put("IN_REVIEW", 0);
        map.put("RESOLVED", 0);
        map.put("REJECTED", 0);
        return map;
    }

    /**
     * Get dashboard statistics summary for a DGM.
     */
    @Transactional(readOnly = true)
    public DgmDashboardStats getDashboardStats(Long dgmUserId) {
        Dgm dgm = getDgmByUserId(dgmUserId);
        List<Hostel> hostels = getHostelsForDgm(dgm.getId());
        if (hostels.isEmpty()) {
            return new DgmDashboardStats(0, 0, 0, 0, 0);
        }

        List<Long> hostelIds = hostels.stream().map(Hostel::getId).collect(Collectors.toList());
        List<Complaint> allComplaints = complaintRepository.findByHostelIds(hostelIds);

        long totalComplaints = allComplaints.size();
        long escalatedCount = allComplaints.stream().filter(Complaint::isEscalated).count();
        long overdueCount = allComplaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.Overdue || c.getOverdueAt() != null)
                .count();
        long pendingCount = allComplaints.stream()
                .filter(c -> c.getStatus() != ComplaintStatus.Resolved
                          && c.getStatus() != ComplaintStatus.Rejected)
                .count();

        return new DgmDashboardStats(totalComplaints, escalatedCount, overdueCount, pendingCount, hostels.size());
    }

    /**
     * Inner class for DGM dashboard statistics summary.
     */
    public static class DgmDashboardStats {
        public final long totalComplaints;
        public final long escalatedComplaints;
        public final long overdueComplaints;
        public final long pendingComplaints;
        public final int hostelsManaged;

        public DgmDashboardStats(long totalComplaints, long escalatedComplaints,
                                long overdueComplaints, long pendingComplaints, int hostelsManaged) {
            this.totalComplaints = totalComplaints;
            this.escalatedComplaints = escalatedComplaints;
            this.overdueComplaints = overdueComplaints;
            this.pendingComplaints = pendingComplaints;
            this.hostelsManaged = hostelsManaged;
        }
    }
}
