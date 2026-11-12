package com.hostelsync.backend.service;

import com.hostelsync.backend.dto.DashboardStatsResponse;
import com.hostelsync.backend.entity.Complaint;
import com.hostelsync.backend.entity.ComplaintStatus;
import com.hostelsync.backend.entity.ComplaintType;
import com.hostelsync.backend.entity.Room;
import com.hostelsync.backend.entity.StudentStatus;
import com.hostelsync.backend.repository.ComplaintRepository;
import com.hostelsync.backend.repository.RoomRepository;
import com.hostelsync.backend.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final ComplaintRepository complaintRepository;
    private DemoScopeService demoScopeService;

    public DashboardService(StudentRepository studentRepository,
                             RoomRepository roomRepository,
                             ComplaintRepository complaintRepository) {
        this.studentRepository = studentRepository;
        this.roomRepository = roomRepository;
        this.complaintRepository = complaintRepository;
    }

    @Autowired(required = false)
    public void setDemoScopeService(DemoScopeService demoScopeService) {
        this.demoScopeService = demoScopeService;
    }

    public DashboardStatsResponse getStats() {
        if (demoScopeService != null && demoScopeService.isCurrentCallerDemo()) {
            return getDemoScopedStats(demoScopeService.requireDemoHostelId());
        }

        if (demoScopeService != null && demoScopeService.findDemoHostel().isPresent()) {
            return getInstitutionalScopedStats(demoScopeService.findDemoHostel().get().getId());
        }

        return getGlobalStats();
    }

    private DashboardStatsResponse getGlobalStats() {
        DashboardStatsResponse stats = new DashboardStatsResponse();

        // Students
        stats.setTotalStudents(studentRepository.countByStatus(StudentStatus.Active));

        // Rooms
        stats.setTotalRooms(roomRepository.countByActiveTrue());

        // Available beds = total capacity - total active occupied
        long totalCapacity = roomRepository.sumCapacity();
        long totalOccupied = studentRepository.countByStatus(StudentStatus.Active);
        stats.setAvailableBeds(Math.max(0, totalCapacity - totalOccupied));

        // Complaints
        stats.setOpenComplaints(complaintRepository.countOpenComplaints());
        stats.setResolvedComplaints(complaintRepository.countByStatus(ComplaintStatus.Resolved));

        // By type — keep the same keys the frontend expects
        Map<String, Long> byType = new LinkedHashMap<>();
        byType.put("Mess",             complaintRepository.countByType(ComplaintType.Mess));
        byType.put("Cleaning",         complaintRepository.countByType(ComplaintType.Cleaning));
        byType.put("Room Maintenance", complaintRepository.countByType(ComplaintType.Room_Maintenance));
        byType.put("Electricity",      complaintRepository.countByType(ComplaintType.Electricity));
        byType.put("Water",            complaintRepository.countByType(ComplaintType.Water));
        byType.put("Plumbing",         complaintRepository.countByType(ComplaintType.Plumbing));
        byType.put("Other",            complaintRepository.countByType(ComplaintType.Other));
        stats.setComplaintsByType(byType);

        // By status
        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("Submitted",    complaintRepository.countByStatus(ComplaintStatus.Submitted));
        byStatus.put("Under Review", complaintRepository.countByStatus(ComplaintStatus.Under_Review));
        byStatus.put("In Progress",  complaintRepository.countByStatus(ComplaintStatus.In_Progress));
        byStatus.put("Resolved",     complaintRepository.countByStatus(ComplaintStatus.Resolved));
        byStatus.put("Rejected",     complaintRepository.countByStatus(ComplaintStatus.Rejected));
        stats.setComplaintsByStatus(byStatus);

        return stats;
    }

    private DashboardStatsResponse getInstitutionalScopedStats(Long demoHostelId) {
        DashboardStatsResponse stats = new DashboardStatsResponse();

        long globalActiveStudents = studentRepository.countByStatus(StudentStatus.Active);
        long demoActiveStudents = studentRepository.countByRoomHostelIdAndStatus(demoHostelId, StudentStatus.Active);
        long institutionalStudents = Math.max(0, globalActiveStudents - demoActiveStudents);
        stats.setTotalStudents(institutionalStudents);

        List<Room> demoRooms = roomRepository.findByHostelId(demoHostelId);
        long demoActiveRoomCount = demoRooms.stream().filter(Room::isActive).count();
        long institutionalRooms = Math.max(0, roomRepository.countByActiveTrue() - demoActiveRoomCount);
        stats.setTotalRooms(institutionalRooms);

        long demoCapacity = demoRooms.stream().filter(Room::isActive).mapToLong(Room::getCapacity).sum();
        long institutionalCapacity = Math.max(0, roomRepository.sumCapacity() - demoCapacity);
        stats.setAvailableBeds(Math.max(0, institutionalCapacity - institutionalStudents));

        List<Complaint> demoComplaints = complaintRepository.findByHostelId(demoHostelId);
        long demoOpen = demoComplaints.stream()
                .filter(c -> c.getStatus() != ComplaintStatus.Resolved && c.getStatus() != ComplaintStatus.Rejected)
                .count();
        long demoResolved = demoComplaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.Resolved)
                .count();

        stats.setOpenComplaints(Math.max(0, complaintRepository.countOpenComplaints() - demoOpen));
        stats.setResolvedComplaints(Math.max(0, complaintRepository.countByStatus(ComplaintStatus.Resolved) - demoResolved));

        Map<String, Long> byType = new LinkedHashMap<>();
        byType.put("Mess",             Math.max(0, complaintRepository.countByType(ComplaintType.Mess) - countByType(demoComplaints, ComplaintType.Mess)));
        byType.put("Cleaning",         Math.max(0, complaintRepository.countByType(ComplaintType.Cleaning) - countByType(demoComplaints, ComplaintType.Cleaning)));
        byType.put("Room Maintenance", Math.max(0, complaintRepository.countByType(ComplaintType.Room_Maintenance) - countByType(demoComplaints, ComplaintType.Room_Maintenance)));
        byType.put("Electricity",      Math.max(0, complaintRepository.countByType(ComplaintType.Electricity) - countByType(demoComplaints, ComplaintType.Electricity)));
        byType.put("Water",            Math.max(0, complaintRepository.countByType(ComplaintType.Water) - countByType(demoComplaints, ComplaintType.Water)));
        byType.put("Plumbing",         Math.max(0, complaintRepository.countByType(ComplaintType.Plumbing) - countByType(demoComplaints, ComplaintType.Plumbing)));
        byType.put("Other",            Math.max(0, complaintRepository.countByType(ComplaintType.Other) - countByType(demoComplaints, ComplaintType.Other)));
        stats.setComplaintsByType(byType);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("Submitted",    Math.max(0, complaintRepository.countByStatus(ComplaintStatus.Submitted) - countByStatus(demoComplaints, ComplaintStatus.Submitted)));
        byStatus.put("Under Review", Math.max(0, complaintRepository.countByStatus(ComplaintStatus.Under_Review) - countByStatus(demoComplaints, ComplaintStatus.Under_Review)));
        byStatus.put("In Progress",  Math.max(0, complaintRepository.countByStatus(ComplaintStatus.In_Progress) - countByStatus(demoComplaints, ComplaintStatus.In_Progress)));
        byStatus.put("Resolved",     Math.max(0, complaintRepository.countByStatus(ComplaintStatus.Resolved) - countByStatus(demoComplaints, ComplaintStatus.Resolved)));
        byStatus.put("Rejected",     Math.max(0, complaintRepository.countByStatus(ComplaintStatus.Rejected) - countByStatus(demoComplaints, ComplaintStatus.Rejected)));
        stats.setComplaintsByStatus(byStatus);

        return stats;
    }

    private DashboardStatsResponse getDemoScopedStats(Long demoHostelId) {
        DashboardStatsResponse stats = new DashboardStatsResponse();

        long activeStudents = studentRepository.countByRoomHostelIdAndStatus(demoHostelId, StudentStatus.Active);
        stats.setTotalStudents(activeStudents);

        List<Room> hostelRooms = roomRepository.findByHostelId(demoHostelId);
        long activeRooms = hostelRooms.stream().filter(Room::isActive).count();
        long totalCapacity = hostelRooms.stream()
                .filter(Room::isActive)
                .mapToLong(Room::getCapacity)
                .sum();
        stats.setTotalRooms(activeRooms);
        stats.setAvailableBeds(Math.max(0, totalCapacity - activeStudents));

        List<Complaint> hostelComplaints = complaintRepository.findByHostelId(demoHostelId);
        long openComplaints = hostelComplaints.stream()
                .filter(c -> c.getStatus() != ComplaintStatus.Resolved && c.getStatus() != ComplaintStatus.Rejected)
                .count();
        long resolvedComplaints = hostelComplaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.Resolved)
                .count();
        stats.setOpenComplaints(openComplaints);
        stats.setResolvedComplaints(resolvedComplaints);

        Map<String, Long> byType = new LinkedHashMap<>();
        byType.put("Mess",             countByType(hostelComplaints, ComplaintType.Mess));
        byType.put("Cleaning",         countByType(hostelComplaints, ComplaintType.Cleaning));
        byType.put("Room Maintenance", countByType(hostelComplaints, ComplaintType.Room_Maintenance));
        byType.put("Electricity",      countByType(hostelComplaints, ComplaintType.Electricity));
        byType.put("Water",            countByType(hostelComplaints, ComplaintType.Water));
        byType.put("Plumbing",         countByType(hostelComplaints, ComplaintType.Plumbing));
        byType.put("Other",            countByType(hostelComplaints, ComplaintType.Other));
        stats.setComplaintsByType(byType);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        byStatus.put("Submitted",    countByStatus(hostelComplaints, ComplaintStatus.Submitted));
        byStatus.put("Under Review", countByStatus(hostelComplaints, ComplaintStatus.Under_Review));
        byStatus.put("In Progress",  countByStatus(hostelComplaints, ComplaintStatus.In_Progress));
        byStatus.put("Resolved",     countByStatus(hostelComplaints, ComplaintStatus.Resolved));
        byStatus.put("Rejected",     countByStatus(hostelComplaints, ComplaintStatus.Rejected));
        stats.setComplaintsByStatus(byStatus);

        return stats;
    }

    private long countByType(List<Complaint> complaints, ComplaintType type) {
        return complaints.stream().filter(c -> c.getType() == type).count();
    }

    private long countByStatus(List<Complaint> complaints, ComplaintStatus status) {
        return complaints.stream().filter(c -> c.getStatus() == status).count();
    }
}
