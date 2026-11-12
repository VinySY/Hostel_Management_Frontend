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
    private final com.hostelsync.backend.repository.RoomFurnitureRepository roomFurnitureRepository;

    public MappingService(StudentRepository studentRepository,
                          com.hostelsync.backend.repository.RoomFurnitureRepository roomFurnitureRepository) {
        this.studentRepository = studentRepository;
        this.roomFurnitureRepository = roomFurnitureRepository;
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

        dto.setAvatarUrl(student.getUser().getAvatarUrl());

        if (student.getRoom() != null) {
            Room room = student.getRoom();
            dto.setBlock(room.getBlock());
            dto.setRoomNumber(room.getRoomNumber());
            dto.setRoomId(String.valueOf(room.getId()));
            dto.setFloor(room.getFloor());
            if (room.getHostel() != null) {
                dto.setHostelId(String.valueOf(room.getHostel().getId()));
                dto.setHostelName(room.getHostel().getName());
            } else {
                dto.setHostelId("");
                dto.setHostelName("");
            }
        } else {
            dto.setBlock("");
            dto.setRoomNumber("");
            dto.setRoomId("");
            dto.setFloor(0);
            dto.setHostelId("");
            dto.setHostelName("");
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
        dto.setActive(room.isActive());

        if (room.getHostel() != null) {
            dto.setHostelId(String.valueOf(room.getHostel().getId()));
            dto.setHostelName(room.getHostel().getName());
        } else {
            dto.setHostelId("");
            dto.setHostelName("");
        }

        dto.setCoolingEquipment(room.getCoolingEquipment() != null ? room.getCoolingEquipment().name() : "NONE");
        dto.setCoolingMaintenanceStatus(room.getCoolingMaintenanceStatus() != null ? room.getCoolingMaintenanceStatus().name() : "OPERATIONAL");

        // Derive status string
        if (occupiedCount == 0) {
            dto.setStatus("Available");
        } else if (occupiedCount >= room.getCapacity()) {
            dto.setStatus("Full");
        } else {
            dto.setStatus("Partially Occupied");
        }

        // Furniture inventory and discrepancy calculations
        List<RoomFurniture> furnitureList = roomFurnitureRepository.findByRoom(room);
        java.util.List<RoomFurnitureDto> furnitureDtos = new java.util.ArrayList<>();
        boolean hasShortage = false;
        boolean hasMaintenance = room.getCoolingMaintenanceStatus() != null &&
                room.getCoolingMaintenanceStatus() != CoolingMaintenanceStatus.OPERATIONAL;

        for (FurnitureType ftype : FurnitureType.values()) {
            RoomFurniture found = furnitureList.stream()
                    .filter(f -> f.getFurnitureType() == ftype)
                    .findFirst()
                    .orElse(null);

            int req = room.getCapacity();
            int serv = found != null ? found.getServiceableCount() : req;
            int dam = found != null ? found.getDamagedCount() : 0;
            int miss = found != null ? found.getMissingCount() : 0;
            int repair = found != null ? found.getUnderRepairCount() : 0;
            int shortCount = Math.max(0, req - serv);

            if (shortCount > 0) hasShortage = true;
            if (dam > 0 || miss > 0 || repair > 0 || shortCount > 0) hasMaintenance = true;

            furnitureDtos.add(new RoomFurnitureDto(ftype.name(), req, serv, dam, miss, repair, shortCount));
        }

        dto.setFurnitureInventory(furnitureDtos);
        dto.setHasShortage(hasShortage);
        dto.setHasMaintenanceIssues(hasMaintenance);

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

    public ComplaintResponse toComplaintResponse(Complaint c) {
        return toComplaintResponse(c, "/api/files");
    }

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
        dto.setEscalated(c.isEscalated());
        dto.setEscalatedAt(c.getEscalatedAt() != null ? c.getEscalatedAt().format(ISO) + "Z" : null);
        dto.setSlaDeadline(c.getSlaDeadline() != null ? c.getSlaDeadline().format(ISO) + "Z" : null);
        dto.setOverdueAt(c.getOverdueAt() != null ? c.getOverdueAt().format(ISO) + "Z" : null);

        boolean isTerminal = c.getStatus() == ComplaintStatus.Resolved || c.getStatus() == ComplaintStatus.Rejected;
        boolean pastDeadline = c.getSlaDeadline() != null && c.getSlaDeadline().isBefore(java.time.LocalDateTime.now());
        dto.setSlaBreached(c.getStatus() == ComplaintStatus.Overdue || c.getOverdueAt() != null || (!isTerminal && pastDeadline));

        // Room & hostel info from student
        Room room = c.getStudent().getRoom();
        if (room != null) {
            dto.setRoomId(String.valueOf(room.getId()));
            dto.setRoomNumber(room.getRoomNumber());
            dto.setBlock(room.getBlock());
            dto.setFloor(room.getFloor());
            if (room.getHostel() != null) {
                dto.setHostelId(String.valueOf(room.getHostel().getId()));
                dto.setHostelName(room.getHostel().getName());
            } else {
                dto.setHostelId("");
                dto.setHostelName("");
            }
        } else {
            dto.setRoomId("");
            dto.setRoomNumber("N/A");
            dto.setBlock("N/A");
            dto.setFloor(null);
            dto.setHostelId("");
            dto.setHostelName("");
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
