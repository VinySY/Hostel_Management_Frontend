package com.hostelsync.backend.dto;

public class HostelResponse {

    private Long id;
    private String name;
    private String address;
    private boolean active;
    private long totalRooms;
    private long totalCapacity;
    private long totalOccupancy;
    private long totalStudents;
    private long wardensCount;
    private long activeComplaintsCount;

    public HostelResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public long getTotalRooms() { return totalRooms; }
    public void setTotalRooms(long totalRooms) { this.totalRooms = totalRooms; }

    public long getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(long totalCapacity) { this.totalCapacity = totalCapacity; }

    public long getTotalOccupancy() { return totalOccupancy; }
    public void setTotalOccupancy(long totalOccupancy) { this.totalOccupancy = totalOccupancy; }

    public long getTotalStudents() { return totalStudents; }
    public void setTotalStudents(long totalStudents) { this.totalStudents = totalStudents; }

    public long getWardensCount() { return wardensCount; }
    public void setWardensCount(long wardensCount) { this.wardensCount = wardensCount; }

    public long getActiveComplaintsCount() { return activeComplaintsCount; }
    public void setActiveComplaintsCount(long activeComplaintsCount) { this.activeComplaintsCount = activeComplaintsCount; }
}
