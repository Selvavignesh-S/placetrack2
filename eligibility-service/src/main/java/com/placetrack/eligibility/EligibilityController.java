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

    public EligibilityController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    @PostMapping("/check")
    public EligibilityResponse check(@Valid @RequestBody EligibilityRequest request) {
        return eligibilityService.check(request);
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "UP");
    }
}
