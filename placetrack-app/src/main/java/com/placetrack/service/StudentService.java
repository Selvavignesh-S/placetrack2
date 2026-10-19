package com.placetrack.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.placetrack.model.Student;
import com.placetrack.repository.StudentRepository;

/** Student profile reads and updates, and the student counts for the dashboard. */
@Service
public class StudentService {

    private final StudentRepository studentRepository;

    // Constructor injection: Spring passes in the beans this class needs
    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    // One student, or 404 if the id does not exist
    public Student getById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }

    // Insert or update (used by the profile page)
    public Student save(Student student) {
        return studentRepository.save(student);
    }

    // Dashboard: total students
    public long countStudents() {
        return studentRepository.count();
    }

    // Dashboard: placed students
    public long countPlaced() {
        return studentRepository.countByPlacedTrue();
    }
}
