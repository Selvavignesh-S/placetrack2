package com.placetrack.model;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** A placement drive: one company, one role, the eligibility criteria and a deadline. */
@Entity
@Table(name = "drives")
public class Drive {

    /** Departments shown as checkboxes and dropdowns everywhere in the app. */
    public static final List<String> DEPARTMENTS = List.of("CSE", "IT", "ECE", "EEE", "MECH", "CIVIL", "AIDS");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Many drives belong to one company: a foreign key column company_id in "drives". */
    @ManyToOne(optional = false)
    private Company company;

    @NotBlank
    @Column(nullable = false)
    private String roleTitle;

    @DecimalMin("0.1")
    private double packageLpa;

    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private double minCgpa;

    @Min(0)
    private int maxBacklogs;

    /** Comma-separated, for example "CSE,IT". */
    @NotBlank(message = "pick at least one department")
    @Column(nullable = false)
    private String allowedDepartments;

    /** Lets the HTML input type="date" (yyyy-MM-dd) bind to LocalDate. */
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = false)
    private LocalDate deadline;

    public Drive() {
    }

    public Drive(Company company, String roleTitle, double packageLpa, double minCgpa, int maxBacklogs,
            String allowedDepartments, LocalDate deadline) {
        this.company = company;
        this.roleTitle = roleTitle;
        this.packageLpa = packageLpa;
        this.minCgpa = minCgpa;
        this.maxBacklogs = maxBacklogs;
        this.allowedDepartments = allowedDepartments;
        this.deadline = deadline;
    }

    /** "CSE,IT" becomes ["CSE", "IT"]. */
    public List<String> getDepartmentList() {
        if (allowedDepartments == null || allowedDepartments.isBlank()) {
            return List.of();
        }
        return List.of(allowedDepartments.split(","));
    }

    /** A drive accepts applications until the end of its deadline day. */
    public boolean isOpen() {
        return deadline != null && !deadline.isBefore(LocalDate.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getRoleTitle() {
        return roleTitle;
    }

    public void setRoleTitle(String roleTitle) {
        this.roleTitle = roleTitle;
    }

    public double getPackageLpa() {
        return packageLpa;
    }

    public void setPackageLpa(double packageLpa) {
        this.packageLpa = packageLpa;
    }

    public double getMinCgpa() {
        return minCgpa;
    }

    public void setMinCgpa(double minCgpa) {
        this.minCgpa = minCgpa;
    }

    public int getMaxBacklogs() {
        return maxBacklogs;
    }

    public void setMaxBacklogs(int maxBacklogs) {
        this.maxBacklogs = maxBacklogs;
    }

    public String getAllowedDepartments() {
        return allowedDepartments;
    }

    public void setAllowedDepartments(String allowedDepartments) {
        this.allowedDepartments = allowedDepartments;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }
}
