package com.hostelsync.backend.service;

import com.hostelsync.backend.dto.*;
import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Converts JPA entities to DTO responses.
 * Keeping this separate keeps service classes readable.
 */
@Service
public class MappingService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final StudentRepository studentRepository;

    public MappingService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    // ─── Student ──────────────────────────────────────────────────────────────

    public StudentResponse toStudentResponse(Student student) {
        StudentResponse dto = new StudentResponse();
        dto.setId(String.valueOf(student.getId()));
        dto.setStudentId(student.getStudentId());
        dto.setName(student.getUser().getName());
        dto.setEmail(student.getUser().getEmail());
        dto.setPhone(student.getPhone());
        dto.setGender(student.getGender());
        dto.setCourse(student.getCourse());
        dto.setYear(student.getYear());
        dto.setStatus(student.getStatus().name());
        dto.setGuardianName(student.getGuardianName());
        dto.setGuardianPhone(student.getGuardianPhone());

        if (student.getRoom() != null) {
            Room room = student.getRoom();
            dto.setBlock(room.getBlock());
            dto.setRoomNumber(room.getRoomNumber());
            dto.setRoomId(String.valueOf(room.getId()));
        } else {
            dto.setBlock("");
            dto.setRoomNumber("");
            dto.setRoomId("");
        }

        return dto;
    }

    // ─── Room ─────────────────────────────────────────────────────────────────

    public RoomResponse toRoomResponse(Room room) {
        long occupiedCount = studentRepository.countByRoomAndStatus(room, StudentStatus.Active);

        RoomResponse dto = new RoomResponse();
        dto.setId(String.valueOf(room.getId()));
        dto.setRoomNumber(room.getRoomNumber());
        dto.setBlock(room.getBlock());
        dto.setFloor(room.getFloor());
        dto.setCapacity(room.getCapacity());
        dto.setOccupied((int) occupiedCount);

        // Derive status string
        if (occupiedCount == 0) {
            dto.setStatus("Available");
        } else if (occupiedCount >= room.getCapacity()) {
            dto.setStatus("Full");
        } else {
            dto.setStatus("Partially Occupied");
        }

        // Occupants — only Active students
        List<RoomOccupantDto> occupants = studentRepository
                .findByRoom(room)
                .stream()
                .filter(s -> s.getStatus() == StudentStatus.Active)
                .map(s -> new RoomOccupantDto(
                        s.getStudentId(),
                        s.getUser().getName(),
                        s.getUser().getEmail(),
                        s.getCourse(),
                        s.getYear(),
                        s.getPhone()))
                .toList();

        dto.setOccupants(occupants);
        return dto;
    }

    // ─── Complaint ────────────────────────────────────────────────────────────

    public ComplaintResponse toComplaintResponse(Complaint c, String fileBaseUrl) {
        ComplaintResponse dto = new ComplaintResponse();
        dto.setId(String.valueOf(c.getId()));
        dto.setComplaintNumber(c.getComplaintNumber());
        dto.setStudentId(c.getStudent().getStudentId());
        dto.setStudentName(c.getStudent().getUser().getName());
        dto.setType(c.getType().getDisplayName());
        dto.setSubject(c.getSubject());
        dto.setDescription(c.getDescription());
        dto.setLocation(c.getLocation());
        dto.setPriority(c.getPriority().name());
        dto.setStatus(c.getStatus().getDisplayName());
        dto.setAdminNotes(c.getAdminNotes() != null ? c.getAdminNotes() : "");
        dto.setCreatedAt(c.getCreatedAt().format(ISO) + "Z");
        dto.setUpdatedAt(c.getUpdatedAt().format(ISO) + "Z");

        // Room info from student
        Room room = c.getStudent().getRoom();
        if (room != null) {
            dto.setRoomId(String.valueOf(room.getId()));
            dto.setRoomNumber(room.getRoomNumber());
            dto.setBlock(room.getBlock());
        } else {
            dto.setRoomId("");
            dto.setRoomNumber("N/A");
            dto.setBlock("N/A");
        }

        // Attachments
        List<AttachmentDto> attachments = c.getAttachments().stream()
                .map(att -> new AttachmentDto(
                        String.valueOf(att.getId()),
                        att.getOriginalFileName(),
                        att.getFileSize(),
                        att.getContentType(),
                        fileBaseUrl + "/" + att.getStoredFileName()))
                .toList();
        dto.setAttachments(attachments);

        // Timeline
        List<TimelineItemDto> timeline = c.getTimeline().stream()
                .map(t -> new TimelineItemDto(
                        t.getStatus().getDisplayName(),
                        t.getTimestamp().format(ISO) + "Z",
                        t.getNote()))
                .toList();
        dto.setTimeline(timeline);

        return dto;
    }
}
