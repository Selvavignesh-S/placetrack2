package com.placetrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.placetrack.model.Student;

/** CRUD for students. Queries here join "students" with "users" automatically (JOINED inheritance). */
public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByRegNo(String regNo);
}
