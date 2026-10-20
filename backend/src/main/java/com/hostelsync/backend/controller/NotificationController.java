package com.hostelsync.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Stub notification endpoints so the frontend doesn't hit 404.
 * The frontend calls these but they are not critical for the core workflow.
 * A full notification system can be built on top of this scaffold.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @GetMapping
    public ResponseEntity<List<Object>> getNotifications() {
        // Return empty list — frontend handles empty gracefully
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable String id) {
        // No-op until a notification system is implemented
        return ResponseEntity.ok().build();
    }
}
