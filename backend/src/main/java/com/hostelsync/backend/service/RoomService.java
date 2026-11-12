package com.hostelsync.backend.service;

import com.hostelsync.backend.dto.*;
import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.BadRequestException;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final HostelRepository hostelRepository;
    private final RoomFurnitureRepository roomFurnitureRepository;
    private final RoomFurnitureHistoryRepository roomFurnitureHistoryRepository;
    private final MappingService mappingService;

    public RoomService(RoomRepository roomRepository,
                       StudentRepository studentRepository,
                       HostelRepository hostelRepository,
                       RoomFurnitureRepository roomFurnitureRepository,
                       RoomFurnitureHistoryRepository roomFurnitureHistoryRepository,
                       MappingService mappingService) {
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.hostelRepository = hostelRepository;
        this.roomFurnitureRepository = roomFurnitureRepository;
        this.roomFurnitureHistoryRepository = roomFurnitureHistoryRepository;
        this.mappingService = mappingService;
    }

    /** All rooms */
    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll()
                .stream()
                .map(mappingService::toRoomResponse)
                .toList();
    }

    /** Single room by numeric DB id */
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));
        return mappingService.toRoomResponse(room);
    }

    /** Available rooms (not full) */
    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRooms() {
        return roomRepository.findAvailableRooms(StudentStatus.Active)
                .stream()
                .map(mappingService::toRoomResponse)
                .toList();
    }

    /** Find Room entity by id (used internally by StudentService) */
    @Transactional(readOnly = true)
    public Room findEntityById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));
    }

    /**
     * Check whether a room has space for one more active student.
     */
    @Transactional(readOnly = true)
    public boolean hasCapacity(Room room) {
        long current = studentRepository.countByRoomAndStatus(room, StudentStatus.Active);
        return current < room.getCapacity();
    }

    /**
     * Create a new room with baseline furniture setup
     */
    public RoomResponse createRoom(RoomCreateRequest request, String initiatedBy) {
        validateFloor(request.getFloor());
        validateCapacity(request.getCapacity());

        if (roomRepository.existsByRoomNumberAndHostelId(request.getRoomNumber(), request.getHostelId())) {
            throw new IllegalArgumentException("Room number '" + request.getRoomNumber() + "' already exists in this hostel");
        }

        Hostel hostel = hostelRepository.findById(request.getHostelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hostel", request.getHostelId()));

        Room room = new Room();
        room.setRoomNumber(request.getRoomNumber().trim());
        room.setBlock(request.getBlock().trim());
        room.setFloor(request.getFloor());
        room.setCapacity(request.getCapacity());
        room.setHostel(hostel);
        room.setActive(true);

        if (request.getCoolingEquipment() != null) {
            try {
                room.setCoolingEquipment(CoolingEquipmentType.valueOf(request.getCoolingEquipment().toUpperCase()));
            } catch (Exception e) {
                room.setCoolingEquipment(CoolingEquipmentType.NONE);
            }
        }
        room.setCoolingMaintenanceStatus(CoolingMaintenanceStatus.OPERATIONAL);

        Room savedRoom = roomRepository.save(room);

        // Baseline furniture inventory (1 bed, 1 study table, 1 locker per capacity)
        for (FurnitureType fType : FurnitureType.values()) {
            RoomFurniture rf = new RoomFurniture(savedRoom, fType, savedRoom.getCapacity());
            rf.setServiceableCount(savedRoom.getCapacity());
            rf.setDamagedCount(0);
            rf.setMissingCount(0);
            rf.setUnderRepairCount(0);
            roomFurnitureRepository.save(rf);

            RoomFurnitureHistory history = new RoomFurnitureHistory(
                    savedRoom,
                    fType,
                    FurnitureActionType.INITIAL_SETUP,
                    savedRoom.getCapacity(),
                    "Initial baseline inventory for room capacity " + savedRoom.getCapacity(),
                    initiatedBy != null ? initiatedBy : "SYSTEM"
            );
            roomFurnitureHistoryRepository.save(history);
        }

        return mappingService.toRoomResponse(savedRoom);
    }

    /**
     * Update room configuration, cooling equipment, or capacity
     */
    public RoomResponse updateRoom(Long id, RoomUpdateRequest request, String initiatedBy) {
        Room room = findEntityById(id);

        if (request.getBlock() != null && !request.getBlock().isBlank()) {
            room.setBlock(request.getBlock().trim());
        }
        if (request.getFloor() != null) {
            validateFloor(request.getFloor());
            room.setFloor(request.getFloor());
        }
        if (request.getActive() != null) {
            room.setActive(request.getActive());
        }
        if (request.getCoolingEquipment() != null) {
            try {
                room.setCoolingEquipment(CoolingEquipmentType.valueOf(request.getCoolingEquipment().toUpperCase()));
            } catch (Exception ignored) {}
        }
        if (request.getCoolingMaintenanceStatus() != null) {
            try {
                room.setCoolingMaintenanceStatus(CoolingMaintenanceStatus.valueOf(request.getCoolingMaintenanceStatus().toUpperCase()));
            } catch (Exception ignored) {}
        }

        if (request.getCapacity() != null) {
            validateCapacity(request.getCapacity());
        }
        if (request.getCapacity() != null && request.getCapacity() != room.getCapacity()) {
            long currentOccupancy = studentRepository.countByRoomAndStatus(room, StudentStatus.Active);
            if (request.getCapacity() < currentOccupancy) {
                throw new IllegalArgumentException("Cannot reduce capacity to " + request.getCapacity() + " because current occupancy is " + currentOccupancy);
            }

            int oldCapacity = room.getCapacity();
            int newCapacity = request.getCapacity();
            room.setCapacity(newCapacity);

            // Dynamically recalculate required furniture baseline without wiping history
            for (FurnitureType fType : FurnitureType.values()) {
                RoomFurniture rf = roomFurnitureRepository.findByRoomAndFurnitureType(room, fType)
                        .orElseGet(() -> new RoomFurniture(room, fType, newCapacity));

                if (newCapacity > oldCapacity) {
                    rf.setServiceableCount(rf.getServiceableCount() + (newCapacity - oldCapacity));
                }
                roomFurnitureRepository.save(rf);

                RoomFurnitureHistory history = new RoomFurnitureHistory(
                        room,
                        fType,
                        FurnitureActionType.CAPACITY_ADJUSTED,
                        newCapacity - oldCapacity,
                        "Room capacity updated from " + oldCapacity + " to " + newCapacity,
                        initiatedBy != null ? initiatedBy : "SYSTEM"
                );
                roomFurnitureHistoryRepository.save(history);
            }
        }

        Room updated = roomRepository.save(room);
        return mappingService.toRoomResponse(updated);
    }

    /**
     * Update furniture inventory state for a specific room and furniture type
     */
    public RoomResponse updateFurniture(Long roomId, FurnitureUpdateRequest request, String initiatedBy) {
        Room room = findEntityById(roomId);

        FurnitureType fType;
        try {
            fType = FurnitureType.valueOf(request.getFurnitureType().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid furniture type: " + request.getFurnitureType());
        }

        RoomFurniture rf = roomFurnitureRepository.findByRoomAndFurnitureType(room, fType)
                .orElseGet(() -> new RoomFurniture(room, fType, room.getCapacity()));

        int oldTotal = rf.getServiceableCount() + rf.getDamagedCount() + rf.getMissingCount() + rf.getUnderRepairCount();

        rf.setServiceableCount(request.getServiceableCount());
        rf.setDamagedCount(request.getDamagedCount());
        rf.setMissingCount(request.getMissingCount());
        rf.setUnderRepairCount(request.getUnderRepairCount());

        roomFurnitureRepository.save(rf);

        int newTotal = rf.getServiceableCount() + rf.getDamagedCount() + rf.getMissingCount() + rf.getUnderRepairCount();

        FurnitureActionType actionType = FurnitureActionType.INVENTORY_AUDIT;
        if (request.getActionType() != null) {
            try {
                actionType = FurnitureActionType.valueOf(request.getActionType().toUpperCase());
            } catch (Exception ignored) {}
        }

        String notes = request.getNotes() != null && !request.getNotes().isBlank()
                ? request.getNotes()
                : "Inventory update: " + request.getServiceableCount() + " serviceable, "
                + request.getDamagedCount() + " damaged, "
                + request.getMissingCount() + " missing, "
                + request.getUnderRepairCount() + " under repair";

        RoomFurnitureHistory history = new RoomFurnitureHistory(
                room,
                fType,
                actionType,
                newTotal - oldTotal,
                notes,
                initiatedBy != null ? initiatedBy : "SYSTEM"
        );
        roomFurnitureHistoryRepository.save(history);

        return mappingService.toRoomResponse(room);
    }

    /**
     * Get maintenance & audit history of room furniture
     */
    @Transactional(readOnly = true)
    public List<FurnitureHistoryResponse> getFurnitureHistory(Long roomId) {
        Room room = findEntityById(roomId);
        return roomFurnitureHistoryRepository.findByRoomOrderByRecordedAtDesc(room)
                .stream()
                .map(h -> new FurnitureHistoryResponse(
                        h.getId(),
                        h.getRoom().getId(),
                        h.getRoom().getRoomNumber(),
                        h.getFurnitureType().name(),
                        h.getActionType().name(),
                        h.getQuantityChanged(),
                        h.getNotes(),
                        h.getRecordedBy(),
                        h.getRecordedAt() != null ? h.getRecordedAt().toString() : ""
                ))
                .toList();
    }

    private static void validateFloor(int floor) {
        if (floor < 0) {
            throw new BadRequestException("Floor must be 0 or greater");
        }
    }

    private static void validateCapacity(int capacity) {
        if (capacity < 1 || capacity > 20) {
            throw new BadRequestException("Capacity must be between 1 and 20");
        }
    }
}
