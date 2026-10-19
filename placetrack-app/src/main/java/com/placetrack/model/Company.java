package com.placetrack.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A recruiting company. One company can run many drives. */
@Entity
@Table(name = "companies")
public class Company {

    // Primary key, auto-numbered
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Required, max 100 characters, unique in the table
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true)
    private String name;

    // Optional, e.g. "Software"
    @Size(max = 100)
    private String sector;

    // Used by JPA and the company form
    public Company() {
    }

    // Used by DataSeeder and tests
    public Company(String name, String sector) {
        this.name = name;
        this.sector = sector;
    }

    // --- Getters and setters (used by Spring forms, Thymeleaf and JSON) ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }
}
