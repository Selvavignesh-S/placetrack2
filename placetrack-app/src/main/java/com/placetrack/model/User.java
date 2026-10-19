package com.placetrack.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Base class for every account. JOINED inheritance: common fields live in "users",
 * Student and Coordinator each get their own table joined on the same id.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User {

    // Primary key; the database numbers new rows automatically (IDENTITY)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Shown in lists and the navbar
    @NotBlank
    @Column(nullable = false)
    private String fullName;

    // Login name; unique across all users
    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash, never the plain password. Hidden from JSON. */
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    // JPA needs a no-argument constructor to create objects from rows
    protected User() {
    }

    // Used by the Student and Coordinator constructors
    protected User(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
    }

    // --- Getters and setters (used by Spring forms, Thymeleaf and JSON) ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
