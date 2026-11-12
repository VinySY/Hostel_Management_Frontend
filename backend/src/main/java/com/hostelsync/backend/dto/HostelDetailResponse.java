package com.hostelsync.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class HostelDetailResponse {

    private HostelResponse summary;
    private List<RoomResponse> rooms = new ArrayList<>();
    private List<WardenResponse> wardens = new ArrayList<>();
    private List<ComplaintResponse> recentComplaints = new ArrayList<>();

    public HostelDetailResponse() {}

    public HostelDetailResponse(HostelResponse summary, List<RoomResponse> rooms,
                                List<WardenResponse> wardens, List<ComplaintResponse> recentComplaints) {
        this.summary = summary;
        this.rooms = rooms;
        this.wardens = wardens;
        this.recentComplaints = recentComplaints;
    }

    public HostelResponse getSummary() { return summary; }
    public void setSummary(HostelResponse summary) { this.summary = summary; }

    public List<RoomResponse> getRooms() { return rooms; }
    public void setRooms(List<RoomResponse> rooms) { this.rooms = rooms; }

    public List<WardenResponse> getWardens() { return wardens; }
    public void setWardens(List<WardenResponse> wardens) { this.wardens = wardens; }

    public List<ComplaintResponse> getRecentComplaints() { return recentComplaints; }
    public void setRecentComplaints(List<ComplaintResponse> recentComplaints) { this.recentComplaints = recentComplaints; }
}
