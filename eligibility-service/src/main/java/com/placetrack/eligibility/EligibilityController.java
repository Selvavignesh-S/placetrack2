package com.placetrack.eligibility;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** REST endpoints of the eligibility microservice. Invalid JSON gets Spring's default 400 reply. */
@RestController
@RequestMapping("/eligibility")
public class EligibilityController {

    private final EligibilityService eligibilityService;

    // Constructor injection: Spring passes in the EligibilityService bean
    public EligibilityController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    // POST /eligibility/check: @RequestBody reads the JSON, @Valid checks it (400 if invalid)
    @PostMapping("/check")
    public EligibilityResponse check(@Valid @RequestBody EligibilityRequest request) {
        return eligibilityService.check(request);
    }

    // GET /eligibility/ping: quick "is the service alive?" check
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "UP");
    }
}
