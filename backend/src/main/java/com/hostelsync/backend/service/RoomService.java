package com.hostelsync.backend.service;

import com.hostelsync.backend.dto.RoomResponse;
import com.hostelsync.backend.entity.*;
import com.hostelsync.backend.exception.ResourceNotFoundException;
import com.hostelsync.backend.repository.RoomRepository;
import com.hostelsync.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final MappingService mappingService;

    public RoomService(RoomRepository roomRepository,
                       StudentRepository studentRepository,
                       MappingService mappingService) {
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.mappingService = mappingService;
    }

    /** All rooms */
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll()
                .stream()
                .map(mappingService::toRoomResponse)
                .toList();
    }

    /** Single room by numeric DB id */
    public RoomResponse getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));
        return mappingService.toRoomResponse(room);
    }

    /** Available rooms (not full) */
    public List<RoomResponse> getAvailableRooms() {
        return roomRepository.findAvailableRooms(StudentStatus.Active)
                .stream()
                .map(mappingService::toRoomResponse)
                .toList();
    }

    /** Find Room entity by id (used internally by StudentService) */
    public Room findEntityById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room", id));
    }

    /**
     * Check whether a room has space for one more active student.
     */
    public boolean hasCapacity(Room room) {
        long current = studentRepository.countByRoomAndStatus(room, StudentStatus.Active);
        return current < room.getCapacity();
    }
}
