package com.hostelsync.backend.service;

import com.hostelsync.backend.dto.DashboardStatsResponse;
import com.hostelsync.backend.entity.ComplaintStatus;
import com.hostelsync.backend.entity.ComplaintType;
import com.hostelsync.backend.entity.StudentStatus;
import com.hostelsync.backend.repository.ComplaintRepository;
import com.hostelsync.backend.repository.RoomRepository;
import com.hostelsync.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final ComplaintRepository complaintRepository;

    public DashboardService(StudentRepository studentRepository,
                             RoomRepository roomRepository,
                             ComplaintRepository complaintRepository) {
        this.studentRepository = studentRepository;
        this.roomRepository = roomRepository;
        this.complaintRepository = complaintRepository;
    }

    public DashboardStatsResponse getStats() {
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
}
