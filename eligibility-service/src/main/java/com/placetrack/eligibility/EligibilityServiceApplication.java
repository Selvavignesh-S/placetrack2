package com.placetrack.eligibility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starts the eligibility microservice on port 8081 (see application.properties). */
@SpringBootApplication
public class EligibilityServiceApplication {

    // Starts Spring: creates the beans and the embedded Tomcat web server
    public static void main(String[] args) {
        SpringApplication.run(EligibilityServiceApplication.class, args);
    }
}
