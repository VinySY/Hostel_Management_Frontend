package com.hostelsync.backend.entity;

/**
 * Status of an escalation.
 */
public enum EscalationStatus {
    PENDING,      // Escalation submitted, awaiting DGM review
    IN_REVIEW,    // DGM is reviewing the escalation
    RESOLVED,     // DGM has resolved the escalation
    REJECTED      // Escalation was rejected by DGM
}
