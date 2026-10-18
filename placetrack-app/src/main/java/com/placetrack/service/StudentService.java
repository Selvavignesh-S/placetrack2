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

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public Student getById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }

    public Student save(Student student) {
        return studentRepository.save(student);
    }

    public long countStudents() {
        return studentRepository.count();
    }

    public long countPlaced() {
        return studentRepository.countByPlacedTrue();
    }
}
