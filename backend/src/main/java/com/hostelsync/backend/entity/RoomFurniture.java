package com.hostelsync.backend.entity;

import jakarta.persistence.*;

/**
 * Tracks furniture inventory for a specific room and furniture type.
 * Required quantity = room capacity (1 bed, 1 table, 1 locker per student slot).
 */
@Entity
@Table(name = "room_furniture",
       uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "furniture_type"}))
public class RoomFurniture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "furniture_type", nullable = false, length = 30)
    private FurnitureType furnitureType;

    @Column(name = "serviceable_count", nullable = false)
    private int serviceableCount;

    @Column(name = "damaged_count", nullable = false)
    private int damagedCount = 0;

    @Column(name = "missing_count", nullable = false)
    private int missingCount = 0;

    @Column(name = "under_repair_count", nullable = false)
    private int underRepairCount = 0;

    public RoomFurniture() {}

    public RoomFurniture(Room room, FurnitureType furnitureType, int serviceableCount) {
        this.room = room;
        this.furnitureType = furnitureType;
        this.serviceableCount = serviceableCount;
    }

    public int getRequiredCount() {
        return room != null ? room.getCapacity() : 0;
    }

    public int getShortage() {
        return Math.max(0, getRequiredCount() - serviceableCount);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public FurnitureType getFurnitureType() { return furnitureType; }
    public void setFurnitureType(FurnitureType furnitureType) { this.furnitureType = furnitureType; }

    public int getServiceableCount() { return serviceableCount; }
    public void setServiceableCount(int serviceableCount) { this.serviceableCount = serviceableCount; }

    public int getDamagedCount() { return damagedCount; }
    public void setDamagedCount(int damagedCount) { this.damagedCount = damagedCount; }

    public int getMissingCount() { return missingCount; }
    public void setMissingCount(int missingCount) { this.missingCount = missingCount; }

    public int getUnderRepairCount() { return underRepairCount; }
    public void setUnderRepairCount(int underRepairCount) { this.underRepairCount = underRepairCount; }
}
