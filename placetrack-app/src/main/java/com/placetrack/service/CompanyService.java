package com.placetrack.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.placetrack.model.Company;
import com.placetrack.repository.CompanyRepository;
import com.placetrack.repository.DriveRepository;

/** Company CRUD plus two rules: names are unique, and a company with drives cannot be deleted. */
@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final DriveRepository driveRepository;

    public CompanyService(CompanyRepository companyRepository, DriveRepository driveRepository) {
        this.companyRepository = companyRepository;
        this.driveRepository = driveRepository;
    }

    public List<Company> findAll() {
        return companyRepository.findAll(Sort.by("name"));
    }

    public Company getById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company " + id + " not found"));
    }

    /** Creates when id is null, updates otherwise. */
    public Company save(Company company) {
        Optional<Company> sameName = companyRepository.findByNameIgnoreCase(company.getName());
        if (sameName.isPresent() && !sameName.get().getId().equals(company.getId())) {
            throw new IllegalArgumentException("A company named " + company.getName() + " already exists");
        }
        return companyRepository.save(company);
    }

    public void delete(Long id) {
        if (driveRepository.countByCompanyId(id) > 0) {
            throw new IllegalArgumentException("This company has drives. Delete its drives first.");
        }
        companyRepository.deleteById(id);
    }
}
