package com.placetrack.model;

/** Life cycle of an application. Stored in the database as text (EnumType.STRING). */
public enum ApplicationStatus {
    // Flow: APPLIED → SHORTLISTED → SELECTED. REJECTED (coordinator) and WITHDRAWN (student) are final.
    APPLIED, SHORTLISTED, SELECTED, REJECTED, WITHDRAWN
}
