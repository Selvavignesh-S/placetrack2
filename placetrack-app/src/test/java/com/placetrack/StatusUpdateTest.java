package com.placetrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.placetrack.client.EligibilityClient;
import com.placetrack.exception.ApplyException;
import com.placetrack.model.Application;
import com.placetrack.model.ApplicationStatus;
import com.placetrack.model.Company;
import com.placetrack.model.Drive;
import com.placetrack.model.Student;
import com.placetrack.repository.ApplicationRepository;
import com.placetrack.repository.StudentRepository;
import com.placetrack.service.ApplicationService;
import com.placetrack.service.DriveService;

/** The coordinator's status rules: allowed moves, refused moves, and SELECTED marking the student as placed. */
@ExtendWith(MockitoExtension.class)
class StatusUpdateTest {

    // @Mock = fake objects; we decide what they return with when(...)
    @Mock StudentRepository studentRepository;
    @Mock DriveService driveService;
    @Mock ApplicationRepository applicationRepository;
    @Mock EligibilityClient eligibilityClient;
    @InjectMocks ApplicationService service; // the real service, built with the mocks above

    Student student;
    Application application;

    // Runs before every test: fresh test data
    @BeforeEach
    void setUp() {
        student = new Student("Rohan", "r@x.edu", "R5", "ECE", 9.1, 0);
        Drive drive = new Drive(new Company("Zephyr", "Software"), "Trainee", 4.5, 6, 2, "ECE",
                LocalDate.now().plusDays(5));
        application = new Application(student, drive, ApplicationStatus.APPLIED);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
    }

    // Normal path; SELECTED also sets placed
    @Test
    void appliedCanBeShortlistedThenSelected() {
        service.updateStatus(1L, ApplicationStatus.SHORTLISTED);
        assertEquals(ApplicationStatus.SHORTLISTED, application.getStatus());
        assertFalse(student.isPlaced());

        service.updateStatus(1L, ApplicationStatus.SELECTED);
        assertEquals(ApplicationStatus.SELECTED, application.getStatus());
        assertTrue(student.isPlaced()); // the second change of the same transaction
    }

    // Skipping SHORTLISTED is refused and nothing changes
    @Test
    void appliedCannotJumpToSelected() {
        assertThrows(ApplyException.class, () -> service.updateStatus(1L, ApplicationStatus.SELECTED));
        assertEquals(ApplicationStatus.APPLIED, application.getStatus());
        assertFalse(student.isPlaced());
    }

    // REJECTED is final
    @Test
    void finalStatusesCannotChange() {
        application.setStatus(ApplicationStatus.REJECTED);
        assertThrows(ApplyException.class, () -> service.updateStatus(1L, ApplicationStatus.SHORTLISTED));
    }

    // WITHDRAWN is only for the student
    @Test
    void coordinatorCannotWithdrawForTheStudent() {
        assertThrows(ApplyException.class, () -> service.updateStatus(1L, ApplicationStatus.WITHDRAWN));
    }
}
