package com.placetrack.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** A placement coordinator (staff) account that manages companies, drives and applicants. */
@Entity
@Table(name = "coordinators")
public class Coordinator extends User {

    @Column(nullable = false, unique = true)
    private String staffId;

    public Coordinator() {
    }

    public Coordinator(String fullName, String email, String staffId) {
        super(fullName, email);
        this.staffId = staffId;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }
}
