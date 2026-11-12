package com.hostelsync.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "warden_floor_assignments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"warden_id", "hostel_id", "floor"}))
public class WardenFloorAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warden_id", nullable = false)
    private Warden warden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hostel_id", nullable = false)
    private Hostel hostel;

    @Column(nullable = false)
    private int floor;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
    }

    public WardenFloorAssignment() {}

    public WardenFloorAssignment(Warden warden, Hostel hostel, int floor) {
        this.warden = warden;
        this.hostel = hostel;
        this.floor = floor;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Warden getWarden() { return warden; }
    public void setWarden(Warden warden) { this.warden = warden; }

    public Hostel getHostel() { return hostel; }
    public void setHostel(Hostel hostel) { this.hostel = hostel; }

    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
}
