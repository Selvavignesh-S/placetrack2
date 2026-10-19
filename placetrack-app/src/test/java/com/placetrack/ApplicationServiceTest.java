package com.placetrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.placetrack.client.EligibilityClient;
import com.placetrack.client.EligibilityResult;
import com.placetrack.exception.ApplyException;
import com.placetrack.exception.EligibilityUnavailableException;
import com.placetrack.model.Application;
import com.placetrack.model.ApplicationStatus;
import com.placetrack.model.Company;
import com.placetrack.model.Drive;
import com.placetrack.model.Student;
import com.placetrack.repository.ApplicationRepository;
import com.placetrack.repository.StudentRepository;
import com.placetrack.service.ApplicationService;
import com.placetrack.service.DriveService;

/** Unit tests of the apply and withdraw rules. Mockito replaces the database and the eligibility service. */
@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    // @Mock = fake objects; we decide what they return with when(...)
    @Mock StudentRepository studentRepository;
    @Mock DriveService driveService;
    @Mock ApplicationRepository applicationRepository;
    @Mock EligibilityClient eligibilityClient;
    @InjectMocks ApplicationService service; // built with the four mocks above (constructor injection)

    Student student;
    Drive drive;

    // Runs before every test: fresh test data
    @BeforeEach
    void setUp() {
        student = new Student("Aarav", "a@x.edu", "R1", "IT", 8.6, 0);
        student.setId(1L);
        drive = new Drive(new Company("Nimbus", "Cloud"), "Cloud Engineer", 9, 7.5, 0, "IT,CSE",
                LocalDate.now().plusDays(7));
        drive.setId(10L);
    }

    // Make the fake repositories return our student and drive
    private void studentAndDriveExist() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(driveService.getById(10L)).thenReturn(drive);
    }

    // Happy path: saved as APPLIED
    @Test
    void eligibleStudentIsSavedAsApplied() {
        studentAndDriveExist();
        when(eligibilityClient.check(student, drive)).thenReturn(new EligibilityResult(true, List.of()));
        when(applicationRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Application saved = service.apply(1L, 10L);

        assertEquals(ApplicationStatus.APPLIED, saved.getStatus());
        assertEquals(student, saved.getStudent());
    }

    // Placed student refused; the eligibility service is never called
    @Test
    void placedStudentIsRefusedBeforeCallingTheService() {
        studentAndDriveExist();
        student.setPlaced(true);

        assertThrows(ApplyException.class, () -> service.apply(1L, 10L));
        verify(eligibilityClient, never()).check(any(), any());
    }

    // Deadline passed
    @Test
    void expiredDriveIsRefused() {
        studentAndDriveExist();
        drive.setDeadline(LocalDate.now().minusDays(1));

        ApplyException e = assertThrows(ApplyException.class, () -> service.apply(1L, 10L));
        assertEquals("The deadline for this drive has passed", e.getMessage());
    }

    // Already applied: nothing saved
    @Test
    void secondApplicationIsRefused() {
        studentAndDriveExist();
        when(applicationRepository.existsByStudentIdAndDriveId(1L, 10L)).thenReturn(true);

        assertThrows(ApplyException.class, () -> service.apply(1L, 10L));
        verify(applicationRepository, never()).save(any());
    }

    // Not eligible: reasons passed on, nothing saved
    @Test
    void ineligibleStudentGetsReasonsAndNothingIsSaved() {
        studentAndDriveExist();
        List<String> reasons = List.of("CGPA 7.2 is below the required 7.5");
        when(eligibilityClient.check(student, drive)).thenReturn(new EligibilityResult(false, reasons));

        ApplyException e = assertThrows(ApplyException.class, () -> service.apply(1L, 10L));
        assertEquals(reasons, e.getReasons());
        verify(applicationRepository, never()).save(any());
    }

    // Eligibility service down: nothing saved
    @Test
    void serviceDownMeansNothingIsSaved() {
        studentAndDriveExist();
        when(eligibilityClient.check(student, drive))
                .thenThrow(new EligibilityUnavailableException(new RuntimeException("connection refused")));

        assertThrows(EligibilityUnavailableException.class, () -> service.apply(1L, 10L));
        verify(applicationRepository, never()).save(any());
    }

    // Withdraw allowed only while APPLIED or SHORTLISTED
    @Test
    void withdrawWorksFromShortlistedButNotFromSelected() {
        Application app = new Application(student, drive, ApplicationStatus.SHORTLISTED);
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(app));

        service.withdraw(5L, 1L);
        assertEquals(ApplicationStatus.WITHDRAWN, app.getStatus());

        app.setStatus(ApplicationStatus.SELECTED);
        assertThrows(ApplyException.class, () -> service.withdraw(5L, 1L));
    }

    // Another student's application looks like 'not found'
    @Test
    void cannotWithdrawSomeoneElsesApplication() {
        Application app = new Application(student, drive, ApplicationStatus.APPLIED);
        when(applicationRepository.findById(5L)).thenReturn(Optional.of(app));

        assertThrows(ResponseStatusException.class, () -> service.withdraw(5L, 99L));
        assertEquals(ApplicationStatus.APPLIED, app.getStatus());
    }
}
