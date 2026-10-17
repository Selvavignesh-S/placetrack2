package com.placetrack.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.placetrack.client.EligibilityClient;
import com.placetrack.client.EligibilityResult;
import com.placetrack.exception.ApplyException;
import com.placetrack.model.Application;
import com.placetrack.model.ApplicationStatus;
import com.placetrack.model.Drive;
import com.placetrack.model.Student;
import com.placetrack.repository.ApplicationRepository;
import com.placetrack.repository.StudentRepository;

/**
 * The apply workflow: checks the rules, asks the eligibility service, saves the application.
 * Every apply call is also logged by ApplyAuditAspect without any logging code here.
 */
@Service
public class ApplicationService {

    private final StudentRepository studentRepository;
    private final DriveService driveService;
    private final ApplicationRepository applicationRepository;
    private final EligibilityClient eligibilityClient;

    public ApplicationService(StudentRepository studentRepository, DriveService driveService,
            ApplicationRepository applicationRepository, EligibilityClient eligibilityClient) {
        this.studentRepository = studentRepository;
        this.driveService = driveService;
        this.applicationRepository = applicationRepository;
        this.eligibilityClient = eligibilityClient;
    }

    /**
     * One transaction: if any rule fails, an exception is thrown and nothing is saved.
     * Rule order: cheap checks in our database first, the network call to the eligibility service last.
     */
    @Transactional
    public Application apply(Long studentId, Long driveId) {
        Student student = getStudent(studentId);
        Drive drive = driveService.getById(driveId);

        if (student.isPlaced()) {
            throw new ApplyException("You are already placed, so you cannot apply to more drives");
        }
        if (!drive.isOpen()) {
            throw new ApplyException("The deadline for this drive has passed");
        }
        if (applicationRepository.existsByStudentIdAndDriveId(studentId, driveId)) {
            throw new ApplyException("You have already applied to this drive");
        }

        EligibilityResult result = eligibilityClient.check(student, drive); // may throw EligibilityUnavailableException
        if (!result.eligible()) {
            throw new ApplyException("You are not eligible for this drive", result.reasons());
        }

        try {
            return applicationRepository.save(new Application(student, drive, ApplicationStatus.APPLIED));
        } catch (DataIntegrityViolationException e) {
            // Two Apply clicks at the same moment: the database's unique (student_id, drive_id) rule stops the second.
            throw new ApplyException("You have already applied to this drive");
        }
    }

    /** "Am I eligible?" without applying. Saves nothing. */
    public EligibilityResult checkEligibility(Long studentId, Long driveId) {
        return eligibilityClient.check(getStudent(studentId), driveService.getById(driveId));
    }

    public List<Application> myApplications(Long studentId) {
        return applicationRepository.findByStudent(studentId);
    }

    /** A student may withdraw their own application while it is APPLIED or SHORTLISTED. */
    @Transactional
    public void withdraw(Long applicationId, Long studentId) {
        Application application = applicationRepository.findById(applicationId)
                .filter(a -> a.getStudent().getId().equals(studentId)) // someone else's: act as if it does not exist
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        ApplicationStatus status = application.getStatus();
        if (status != ApplicationStatus.APPLIED && status != ApplicationStatus.SHORTLISTED) {
            throw new ApplyException("You can only withdraw while the application is APPLIED or SHORTLISTED");
        }
        // No save() needed: the entity is managed inside the transaction, so Hibernate
        // notices the change and writes an UPDATE at commit ("dirty checking").
        application.setStatus(ApplicationStatus.WITHDRAWN);
    }

    private Student getStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }
}
