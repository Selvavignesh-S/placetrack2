package com.placetrack.model;

/** Life cycle of an application. Stored in the database as text (EnumType.STRING). */
public enum ApplicationStatus {
    APPLIED, SHORTLISTED, SELECTED, REJECTED, WITHDRAWN
}
