package com.placetrack.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** One student's application to one drive. The unique constraint blocks applying twice. */
@Entity
@Table(name = "applications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "drive_id"}))
public class Application {

    // Primary key, auto-numbered
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who applied (column student_id)
    @ManyToOne(optional = false)
    private Student student;

    // To which drive (column drive_id)
    @ManyToOne(optional = false)
    private Drive drive;

    // Stored as text: "APPLIED", "SHORTLISTED" ...
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    // When the application was made
    @Column(nullable = false)
    private LocalDateTime appliedAt;

    // Used by JPA
    public Application() {
    }

    // New application: appliedAt is set to now
    public Application(Student student, Drive drive, ApplicationStatus status) {
        this.student = student;
        this.drive = drive;
        this.status = status;
        this.appliedAt = LocalDateTime.now();
    }

    // --- Getters and setters (used by Spring forms, Thymeleaf and JSON) ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Drive getDrive() {
        return drive;
    }

    public void setDrive(Drive drive) {
        this.drive = drive;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}
