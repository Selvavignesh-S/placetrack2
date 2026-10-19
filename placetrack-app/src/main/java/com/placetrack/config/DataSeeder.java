package com.placetrack.config;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.placetrack.model.Application;
import com.placetrack.model.ApplicationStatus;
import com.placetrack.model.Company;
import com.placetrack.model.Coordinator;
import com.placetrack.model.Drive;
import com.placetrack.model.Student;
import com.placetrack.repository.ApplicationRepository;
import com.placetrack.repository.CompanyRepository;
import com.placetrack.repository.DriveRepository;
import com.placetrack.repository.UserRepository;

/** Fills an empty database with demo accounts, companies, drives and applications. Password: Pass@123. */
@Component // Spring creates it and calls run() at startup
@Order(1)   // runs before other startup runners (the demo)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final DriveRepository driveRepository;
    private final ApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, CompanyRepository companyRepository,
            DriveRepository driveRepository, ApplicationRepository applicationRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.driveRepository = driveRepository;
        this.applicationRepository = applicationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Called once when the app starts
    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // already seeded
        }
        // One hash shared by all demo accounts keeps startup fast; real sign-ups each get their own salt.
        String hash = passwordEncoder.encode("Pass@123");

        // --- Users: 2 coordinators, 6 students ---
        Coordinator coord1 = new Coordinator("Dr. Meera Nair", "coord1@placetrack.edu", "STF001");
        Coordinator coord2 = new Coordinator("Prof. Arjun Rao", "coord2@placetrack.edu", "STF002");
        Student s1 = new Student("Aarav Kumar", "student1@placetrack.edu", "23IT001", "IT", 8.6, 0);
        Student s2 = new Student("Diya Menon", "student2@placetrack.edu", "23IT002", "IT", 7.2, 1);
        Student s3 = new Student("Karthik Raman", "student3@placetrack.edu", "23CS003", "CSE", 8.0, 0);
        Student s4 = new Student("Sneha Iyer", "student4@placetrack.edu", "23ME004", "MECH", 7.9, 0);
        Student s5 = new Student("Rohan Das", "student5@placetrack.edu", "23EC005", "ECE", 9.1, 0);
        Student s6 = new Student("Priya Natarajan", "student6@placetrack.edu", "23CS006", "CSE", 6.4, 2);
        s3.setPlaced(true); // selected in D1 below
        List.of(coord1, coord2, s1, s2, s3, s4, s5, s6).forEach(u -> u.setPassword(hash));
        userRepository.saveAll(List.of(coord1, coord2, s1, s2, s3, s4, s5, s6));

        // --- Companies (Helio has no drives, so it can be deleted) ---
        Company zephyr = new Company("Zephyr Systems", "Software");
        Company nimbus = new Company("Nimbus Cloud", "Cloud");
        Company quanta = new Company("Quanta Analytics", "Data and AI");
        Company orion = new Company("Orion Manufacturing", "Manufacturing");
        Company helio = new Company("Helio Power", "Energy");
        companyRepository.saveAll(List.of(zephyr, nimbus, quanta, orion, helio));

        // --- Drives: deadlines relative to today; D4 is already closed ---
        LocalDate today = LocalDate.now();
        Drive d1 = new Drive(zephyr, "Software Engineer Trainee", 4.5, 6.0, 2, "CSE,IT,ECE", today.plusDays(10));
        Drive d2 = new Drive(nimbus, "Cloud Engineer", 9.0, 7.5, 0, "IT,CSE", today.plusDays(7));
        Drive d3 = new Drive(quanta, "Data Scientist", 14.0, 8.5, 0, "CSE,IT,ECE", today.plusDays(14));
        Drive d4 = new Drive(zephyr, "Support Engineer", 3.8, 6.0, 3, String.join(",", Drive.DEPARTMENTS),
                today.minusDays(3));
        Drive d5 = new Drive(orion, "Graduate Engineer Trainee", 4.0, 6.5, 1, "MECH,ECE,EEE,CIVIL",
                today.plusDays(9));
        driveRepository.saveAll(List.of(d1, d2, d3, d4, d5));

        // --- Applications in different states ---
        applicationRepository.saveAll(List.of(
                new Application(s3, d1, ApplicationStatus.SELECTED),
                new Application(s5, d1, ApplicationStatus.SHORTLISTED),
                new Application(s5, d3, ApplicationStatus.APPLIED),
                new Application(s4, d5, ApplicationStatus.APPLIED)));

        log.info("Seeded demo data: 8 users, 5 companies, 5 drives, 4 applications (password Pass@123)");
    }
}
