package com.hostelsync.backend.dto;

import java.util.Map;

/**
 * Dashboard statistics returned by GET /api/dashboard/stats — matches frontend DashboardStats.
 */
public class DashboardStatsResponse {

    private long totalStudents;
    private long totalRooms;
    private long availableBeds;
    private long openComplaints;
    private long resolvedComplaints;
    private Map<String, Long> complaintsByType;
    private Map<String, Long> complaintsByStatus;

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public long getTotalStudents() { return totalStudents; }
    public void setTotalStudents(long totalStudents) { this.totalStudents = totalStudents; }

    public long getTotalRooms() { return totalRooms; }
    public void setTotalRooms(long totalRooms) { this.totalRooms = totalRooms; }

    public long getAvailableBeds() { return availableBeds; }
    public void setAvailableBeds(long availableBeds) { this.availableBeds = availableBeds; }

    public long getOpenComplaints() { return openComplaints; }
    public void setOpenComplaints(long openComplaints) { this.openComplaints = openComplaints; }

    public long getResolvedComplaints() { return resolvedComplaints; }
    public void setResolvedComplaints(long resolvedComplaints) { this.resolvedComplaints = resolvedComplaints; }

    public Map<String, Long> getComplaintsByType() { return complaintsByType; }
    public void setComplaintsByType(Map<String, Long> complaintsByType) { this.complaintsByType = complaintsByType; }

    public Map<String, Long> getComplaintsByStatus() { return complaintsByStatus; }
    public void setComplaintsByStatus(Map<String, Long> complaintsByStatus) { this.complaintsByStatus = complaintsByStatus; }
}
