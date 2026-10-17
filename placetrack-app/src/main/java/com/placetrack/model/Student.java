package com.placetrack.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** A student account with the academic data the eligibility rules look at. */
@Entity
@Table(name = "students")
public class Student extends User {

    @NotBlank
    @Column(nullable = false, unique = true)
    private String regNo;

    @NotBlank
    @Column(nullable = false)
    private String department;

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private double cgpa;

    @Min(0)
    private int backlogs;

    /** True once a coordinator marks the student SELECTED in any drive. */
    private boolean placed;

    public Student() {
    }

    public Student(String fullName, String email, String regNo, String department, double cgpa, int backlogs) {
        super(fullName, email);
        this.regNo = regNo;
        this.department = department;
        this.cgpa = cgpa;
        this.backlogs = backlogs;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public double getCgpa() {
        return cgpa;
    }

    public void setCgpa(double cgpa) {
        this.cgpa = cgpa;
    }

    public int getBacklogs() {
        return backlogs;
    }

    public void setBacklogs(int backlogs) {
        this.backlogs = backlogs;
    }

    public boolean isPlaced() {
        return placed;
    }

    public void setPlaced(boolean placed) {
        this.placed = placed;
    }
}
