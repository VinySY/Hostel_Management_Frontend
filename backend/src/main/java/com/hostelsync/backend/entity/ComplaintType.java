package com.hostelsync.backend.entity;

/**
 * Category of a complaint. Values match the frontend dropdown labels.
 */
public enum ComplaintType {
    Mess,
    Cleaning,
    Room_Maintenance,   // stored as "Room Maintenance" via label; use displayName for serialization
    Electricity,
    Water,
    Plumbing,
    Other;

    /**
     * Returns the display string expected by the frontend (e.g. "Room Maintenance").
     */
    public String getDisplayName() {
        return this.name().replace('_', ' ');
    }

    /**
     * Parses a frontend string like "Room Maintenance" into the enum constant.
     */
    public static ComplaintType fromDisplayName(String displayName) {
        if (displayName == null) return Other;
        return switch (displayName.trim()) {
            case "Mess"             -> Mess;
            case "Cleaning"         -> Cleaning;
            case "Room Maintenance" -> Room_Maintenance;
            case "Electricity"      -> Electricity;
            case "Water"            -> Water;
            case "Plumbing"         -> Plumbing;
            default                 -> Other;
        };
    }
}
