package com.placetrack.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.placetrack.model.User;

/** Finds any account (student or coordinator) by email. Hibernate picks the right subclass. */
public interface UserRepository extends JpaRepository<User, Long> {

    // Derived query: Spring builds "... where email = ?" from the method name. Used at login.
    Optional<User> findByEmail(String email);

    // Is this email taken? Used at registration.
    boolean existsByEmail(String email);
}
