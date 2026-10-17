package com.placetrack.client;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.placetrack.exception.EligibilityUnavailableException;
import com.placetrack.model.Drive;
import com.placetrack.model.Student;

/** Calls the eligibility microservice (port 8081) over REST: POST /eligibility/check. */
@Component
public class EligibilityClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    /** baseUrl comes from eligibility.url in application.properties. */
    public EligibilityClient(RestTemplateBuilder builder, @Value("${eligibility.url}") String baseUrl) {
        // Without timeouts a dead or hanging service would freeze the student's page.
        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(3))
                .build();
        this.baseUrl = baseUrl;
    }

    public EligibilityResult check(Student student, Drive drive) {
        // Spring turns this Map into the JSON body the service expects.
        Map<String, Object> body = Map.of(
                "cgpa", student.getCgpa(),
                "backlogs", student.getBacklogs(),
                "department", student.getDepartment(),
                "minCgpa", drive.getMinCgpa(),
                "maxBacklogs", drive.getMaxBacklogs(),
                "allowedDepartments", drive.getDepartmentList());
        try {
            return restTemplate.postForObject(baseUrl + "/eligibility/check", body, EligibilityResult.class);
        } catch (RestClientException e) {
            // Connection refused, timeout, or an error reply: treat all as "service unavailable".
            throw new EligibilityUnavailableException(e);
        }
    }
}
