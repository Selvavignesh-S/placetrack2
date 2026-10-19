package com.placetrack.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.placetrack.model.Drive;
import com.placetrack.repository.ApplicationRepository;
import com.placetrack.repository.DriveRepository;

/** Drive CRUD, the list of open drives, and the rule that a drive with applications cannot be deleted. */
@Service
public class DriveService {

    private final DriveRepository driveRepository;
    private final ApplicationRepository applicationRepository;

    // Constructor injection: Spring passes in the beans this class needs
    public DriveService(DriveRepository driveRepository, ApplicationRepository applicationRepository) {
        this.driveRepository = driveRepository;
        this.applicationRepository = applicationRepository;
    }

    // All drives, nearest deadline first (coordinator list)
    public List<Drive> findAll() {
        return driveRepository.findAll(Sort.by("deadline"));
    }

    /** Drives whose deadline is today or later. */
    public List<Drive> findOpenDrives() {
        return driveRepository.findOpenDrives(LocalDate.now());
    }

    // One drive, or 404 if the id does not exist
    public Drive getById(Long id) {
        return driveRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Drive " + id + " not found"));
    }

    /** Creates when id is null, updates otherwise. The caller sets the company. */
    public Drive save(Drive drive) {
        return driveRepository.save(drive);
    }

    /** Rows of [companyName, roleTitle, applicationCount] for the dashboard (HQL GROUP BY). */
    public List<Object[]> applicationCounts() {
        return applicationRepository.countApplicationsPerDrive();
    }

    // Rule: a drive that has applications cannot be deleted
    public void delete(Long id) {
        if (applicationRepository.countByDriveId(id) > 0) {
            throw new IllegalArgumentException("Students have applied to this drive, so it cannot be deleted.");
        }
        driveRepository.deleteById(id);
    }
}
