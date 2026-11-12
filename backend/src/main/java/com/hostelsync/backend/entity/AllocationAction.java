package com.hostelsync.backend.entity;

/**
 * Type of room allocation action.
 */
public enum AllocationAction {
    ASSIGNED,      // Student assigned to a room
    TRANSFERRED,   // Student transferred to another room
    DEPARTED,      // Student checked out/left the room
    CANCELLED      // Allocation was cancelled
}
