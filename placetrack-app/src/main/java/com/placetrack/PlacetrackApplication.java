package com.placetrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starts the main PlaceTrack web app on port 8080. */
@SpringBootApplication
public class PlacetrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlacetrackApplication.class, args);
    }
}
