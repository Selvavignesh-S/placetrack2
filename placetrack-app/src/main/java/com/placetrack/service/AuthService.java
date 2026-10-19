package com.placetrack.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.placetrack.config.SessionUser;
import com.placetrack.model.Student;
import com.placetrack.repository.StudentRepository;
import com.placetrack.repository.UserRepository;

/** Login (checks the BCrypt hash) and student registration. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor injection: Spring passes in the beans this class needs
    public AuthService(UserRepository userRepository, StudentRepository studentRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Empty when the email is unknown or the password is wrong (we never say which, on purpose). */
    public Optional<SessionUser> login(String email, String password) {
        return userRepository.findByEmail(email.trim())
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> new SessionUser(user.getId(), user.getFullName(),
                        user instanceof Student ? "STUDENT" : "COORDINATOR"));
    }

    // Checks the password and duplicates, hashes the password, saves the new student
    public void register(Student student, String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (userRepository.existsByEmail(student.getEmail())) {
            throw new IllegalArgumentException("This email is already registered");
        }
        if (studentRepository.existsByRegNo(student.getRegNo())) {
            throw new IllegalArgumentException("This register number is already registered");
        }
        student.setPassword(passwordEncoder.encode(rawPassword)); // store only the BCrypt hash
        studentRepository.save(student);
    }
}
