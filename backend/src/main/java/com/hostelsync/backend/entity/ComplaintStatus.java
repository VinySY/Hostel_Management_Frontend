package com.hostelsync.backend.entity;

/**
 * Lifecycle status of a complaint. Display names match frontend strings exactly.
 */
public enum ComplaintStatus {
    Submitted,
    Under_Review,    // frontend: "Under Review"
    In_Progress,     // frontend: "In Progress"
    Resolved,
    Rejected,
    Overdue;         // Automatically applied when SLA deadline passes

    public String getDisplayName() {
        return this.name().replace('_', ' ');
    }

    public static ComplaintStatus fromDisplayName(String displayName) {
        if (displayName == null) return Submitted;
        return switch (displayName.trim()) {
            case "Submitted"    -> Submitted;
            case "Under Review" -> Under_Review;
            case "In Progress"  -> In_Progress;
            case "Resolved"     -> Resolved;
            case "Rejected"     -> Rejected;
            case "Overdue"      -> Overdue;
            default             -> Submitted;
        };
    }
}
