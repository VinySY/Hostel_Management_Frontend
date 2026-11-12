package com.hostelsync.backend.controller;

import com.hostelsync.backend.dto.FurnitureHistoryResponse;
import com.hostelsync.backend.dto.FurnitureUpdateRequest;
import com.hostelsync.backend.dto.RoomCreateRequest;
import com.hostelsync.backend.dto.RoomResponse;
import com.hostelsync.backend.dto.RoomUpdateRequest;
import com.hostelsync.backend.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Rooms and Furniture Inventory
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @GetMapping("/available")
    public ResponseEntity<List<RoomResponse>> getAvailableRooms() {
        return ResponseEntity.ok(roomService.getAvailableRooms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoomById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('WARDEN', 'DGM')")
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomCreateRequest request,
                                                  Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "ANONYMOUS";
        RoomResponse created = roomService.createRoom(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('WARDEN', 'DGM')")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long id,
                                                  @Valid @RequestBody RoomUpdateRequest request,
                                                  Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "ANONYMOUS";
        return ResponseEntity.ok(roomService.updateRoom(id, request, actor));
    }

    @PutMapping("/{id}/furniture")
    @PreAuthorize("hasAnyRole('WARDEN', 'DGM')")
    public ResponseEntity<RoomResponse> updateFurniture(@PathVariable Long id,
                                                        @Valid @RequestBody FurnitureUpdateRequest request,
                                                        Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "ANONYMOUS";
        return ResponseEntity.ok(roomService.updateFurniture(id, request, actor));
    }

    @GetMapping("/{id}/furniture/history")
    @PreAuthorize("hasAnyRole('WARDEN', 'DGM')")
    public ResponseEntity<List<FurnitureHistoryResponse>> getFurnitureHistory(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getFurnitureHistory(id));
    }
}

