package com.placetrack.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.placetrack.model.Company;

/** CRUD for companies. JpaRepository already gives save, findById, findAll, deleteById, count. */
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByNameIgnoreCase(String name);
}
