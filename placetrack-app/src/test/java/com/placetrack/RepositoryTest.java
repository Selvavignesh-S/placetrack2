package com.placetrack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.placetrack.model.Application;
import com.placetrack.model.ApplicationStatus;
import com.placetrack.model.Company;
import com.placetrack.model.Drive;
import com.placetrack.model.Student;
import com.placetrack.repository.ApplicationRepository;
import com.placetrack.repository.CompanyRepository;
import com.placetrack.repository.DriveRepository;
import com.placetrack.repository.StudentRepository;

/** Runs the HQL queries against a throw-away in-memory H2 database. */
@DataJpaTest
class RepositoryTest {

    @Autowired CompanyRepository companyRepository;
    @Autowired DriveRepository driveRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired ApplicationRepository applicationRepository;

    Drive open;
    Drive expired;

    @BeforeEach
    void setUp() {
        Company c = companyRepository.save(new Company("Acme", "Software"));
        open = driveRepository.save(new Drive(c, "Dev", 5, 6, 1, "CSE,IT", LocalDate.now()));
        expired = driveRepository.save(new Drive(c, "Old", 5, 6, 1, "CSE", LocalDate.now().minusDays(1)));
        Student low = student("Low", "low@x.edu", "R1", 7.0);
        Student high = student("High", "high@x.edu", "R2", 9.0);
        applicationRepository.save(new Application(low, open, ApplicationStatus.APPLIED));
        applicationRepository.save(new Application(high, open, ApplicationStatus.APPLIED));
    }

    private Student student(String name, String email, String regNo, double cgpa) {
        Student s = new Student(name, email, regNo, "CSE", cgpa, 0);
        s.setPassword("hash");
        return studentRepository.save(s);
    }

    @Test
    void openDrivesIncludeTodayAndExcludeExpired() {
        List<Drive> drives = driveRepository.findOpenDrives(LocalDate.now());
        assertEquals(List.of(open.getId()), drives.stream().map(Drive::getId).toList());
    }

    @Test
    void applicantsAreSortedByCgpaDescending() {
        List<Application> apps = applicationRepository.findApplicantsOfDrive(open.getId());
        assertEquals(List.of("High", "Low"), apps.stream().map(a -> a.getStudent().getFullName()).toList());
    }

    @Test
    void countPerDriveKeepsDrivesWithNoApplications() {
        List<Object[]> rows = applicationRepository.countApplicationsPerDrive();
        assertEquals(2, rows.size());
        assertEquals("Dev", rows.get(0)[1]);
        assertEquals(2L, rows.get(0)[2]);
        assertEquals(0L, rows.get(1)[2]);
    }

    @Test
    void duplicateCheckWorks() {
        Long studentId = applicationRepository.findApplicantsOfDrive(open.getId()).get(0).getStudent().getId();
        assertEquals(true, applicationRepository.existsByStudentIdAndDriveId(studentId, open.getId()));
        assertEquals(false, applicationRepository.existsByStudentIdAndDriveId(studentId, expired.getId()));
    }
}
