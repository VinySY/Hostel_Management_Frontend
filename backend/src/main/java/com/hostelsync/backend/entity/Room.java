package com.hostelsync.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A physical room inside a hostel.
 *
 * The 'occupied' count is NOT stored here — it is derived at query time
 * from the number of active Student records assigned to this room.
 * Capacity is the maximum number of beds.
 */
@Entity
@Table(name = "rooms",
       uniqueConstraints = @UniqueConstraint(columnNames = {"room_number", "hostel_id"}))
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 20)
    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    /** e.g. "Block A", "Block B" */
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false)
    private String block;

    @Min(0)
    @Column(nullable = false)
    private int floor;

    @Min(1)
    @Max(20)
    @Column(nullable = false)
    private int capacity;

    /** When false, the room cannot be allocated to students */
    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hostel_id", nullable = false)
    private Hostel hostel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Students currently assigned to this room (active ones)
    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    private List<Student> students = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // -------- Constructors --------

    public Room() {}

    public Room(String roomNumber, String block, int floor, int capacity, Hostel hostel) {
        this.roomNumber = roomNumber;
        this.block = block;
        this.floor = floor;
        this.capacity = capacity;
        this.hostel = hostel;
    }

    // -------- Getters & Setters --------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }

    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Hostel getHostel() { return hostel; }
    public void setHostel(Hostel hostel) { this.hostel = hostel; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public List<Student> getStudents() { return students; }
    public void setStudents(List<Student> students) { this.students = students; }
}
