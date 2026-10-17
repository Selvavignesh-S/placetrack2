package com.placetrack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.placetrack.model.Student;
import com.placetrack.repository.ApplicationRepository;
import com.placetrack.repository.DriveRepository;
import com.placetrack.repository.UserRepository;

/** Starts the whole app on an in-memory database and checks the seed data and inheritance. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:apptest")
class PlacetrackAppTests {

    @Autowired UserRepository userRepository;
    @Autowired DriveRepository driveRepository;
    @Autowired ApplicationRepository applicationRepository;

    @Test
    void seedDataIsLoaded() {
        assertEquals(8, userRepository.count());
        assertEquals(5, driveRepository.count());
        assertEquals(4, applicationRepository.count());
    }

    @Test
    void userRepositoryReturnsTheRightSubclass() {
        assertInstanceOf(Student.class, userRepository.findByEmail("student1@placetrack.edu").orElseThrow());
    }
}
