package com.placetrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starts the main PlaceTrack web app on port 8080. */
@SpringBootApplication
public class PlacetrackApplication {

    // Starts Spring: scans com.placetrack for beans, sets up JPA/H2 and starts Tomcat on 8080
    public static void main(String[] args) {
        SpringApplication.run(PlacetrackApplication.class, args);
    }
}
