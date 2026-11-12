package com.hostelsync.backend.dto;

import java.util.List;
import java.util.Map;

/**
 * Dashboard statistics for DGM.
 */
public class DgmDashboardResponse {

    private int totalComplaints;
    private int pendingEscalations;
    private int resolvedEscalations;
    private int pendingAssignments;
    private Map<String, Integer> complaintsByHostel;
    private Map<String, Integer> escalationStatus;
    private List<DashboardHostelStats> hostelStats;

    public DgmDashboardResponse() {}

    public DgmDashboardResponse(int totalComplaints, int pendingEscalations, int resolvedEscalations,
                               int pendingAssignments, Map<String, Integer> complaintsByHostel,
                               Map<String, Integer> escalationStatus, List<DashboardHostelStats> hostelStats) {
        this.totalComplaints = totalComplaints;
        this.pendingEscalations = pendingEscalations;
        this.resolvedEscalations = resolvedEscalations;
        this.pendingAssignments = pendingAssignments;
        this.complaintsByHostel = complaintsByHostel;
        this.escalationStatus = escalationStatus;
        this.hostelStats = hostelStats;
    }

    public int getTotalComplaints() {
        return totalComplaints;
    }

    public void setTotalComplaints(int totalComplaints) {
        this.totalComplaints = totalComplaints;
    }

    public int getPendingEscalations() {
        return pendingEscalations;
    }

    public void setPendingEscalations(int pendingEscalations) {
        this.pendingEscalations = pendingEscalations;
    }

    public int getResolvedEscalations() {
        return resolvedEscalations;
    }

    public void setResolvedEscalations(int resolvedEscalations) {
        this.resolvedEscalations = resolvedEscalations;
    }

    public int getPendingAssignments() {
        return pendingAssignments;
    }

    public void setPendingAssignments(int pendingAssignments) {
        this.pendingAssignments = pendingAssignments;
    }

    public Map<String, Integer> getComplaintsByHostel() {
        return complaintsByHostel;
    }

    public void setComplaintsByHostel(Map<String, Integer> complaintsByHostel) {
        this.complaintsByHostel = complaintsByHostel;
    }

    public Map<String, Integer> getEscalationStatus() {
        return escalationStatus;
    }

    public void setEscalationStatus(Map<String, Integer> escalationStatus) {
        this.escalationStatus = escalationStatus;
    }

    public List<DashboardHostelStats> getHostelStats() {
        return hostelStats;
    }

    public void setHostelStats(List<DashboardHostelStats> hostelStats) {
        this.hostelStats = hostelStats;
    }

    @Override
    public String toString() {
        return "DgmDashboardResponse{" +
                "totalComplaints=" + totalComplaints +
                ", pendingEscalations=" + pendingEscalations +
                ", resolvedEscalations=" + resolvedEscalations +
                ", pendingAssignments=" + pendingAssignments +
                ", complaintsByHostel=" + complaintsByHostel +
                ", escalationStatus=" + escalationStatus +
                ", hostelStats=" + hostelStats +
                '}';
    }

    /**
     * Stats for individual hostel.
     */
    public static class DashboardHostelStats {
        private Long hostelId;
        private String hostelName;
        private int totalComplaints;
        private int overdueComplaints;
        private int pendingEscalations;
        private int resolvedComplaints;
        private int reopenedComplaints;

        public DashboardHostelStats() {}

        public DashboardHostelStats(Long hostelId, String hostelName, int totalComplaints, int overdueComplaints,
                                   int pendingEscalations, int resolvedComplaints, int reopenedComplaints) {
            this.hostelId = hostelId;
            this.hostelName = hostelName;
            this.totalComplaints = totalComplaints;
            this.overdueComplaints = overdueComplaints;
            this.pendingEscalations = pendingEscalations;
            this.resolvedComplaints = resolvedComplaints;
            this.reopenedComplaints = reopenedComplaints;
        }

        public Long getHostelId() {
            return hostelId;
        }

        public void setHostelId(Long hostelId) {
            this.hostelId = hostelId;
        }

        public String getHostelName() {
            return hostelName;
        }

        public void setHostelName(String hostelName) {
            this.hostelName = hostelName;
        }

        public int getTotalComplaints() {
            return totalComplaints;
        }

        public void setTotalComplaints(int totalComplaints) {
            this.totalComplaints = totalComplaints;
        }

        public int getOverdueComplaints() {
            return overdueComplaints;
        }

        public void setOverdueComplaints(int overdueComplaints) {
            this.overdueComplaints = overdueComplaints;
        }

        public int getPendingEscalations() {
            return pendingEscalations;
        }

        public void setPendingEscalations(int pendingEscalations) {
            this.pendingEscalations = pendingEscalations;
        }

        public int getResolvedComplaints() {
            return resolvedComplaints;
        }

        public void setResolvedComplaints(int resolvedComplaints) {
            this.resolvedComplaints = resolvedComplaints;
        }

        public int getReopenedComplaints() {
            return reopenedComplaints;
        }

        public void setReopenedComplaints(int reopenedComplaints) {
            this.reopenedComplaints = reopenedComplaints;
        }

        @Override
        public String toString() {
            return "DashboardHostelStats{" +
                    "hostelId=" + hostelId +
                    ", hostelName='" + hostelName + '\'' +
                    ", totalComplaints=" + totalComplaints +
                    ", overdueComplaints=" + overdueComplaints +
                    ", pendingEscalations=" + pendingEscalations +
                    ", resolvedComplaints=" + resolvedComplaints +
                    ", reopenedComplaints=" + reopenedComplaints +
                    '}';
        }
    }
}