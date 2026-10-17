package com.placetrack.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.placetrack.model.User;

/** Finds any account (student or coordinator) by email. Hibernate picks the right subclass. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
