package com.placetrack.eligibility;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

/** The three eligibility rules: minimum CGPA, maximum backlogs, allowed department. */
@Service // a Spring bean holding business logic
public class EligibilityService {

    // Runs all rules and collects one reason per failed rule
    public EligibilityResponse check(EligibilityRequest req) {
        List<String> reasons = new ArrayList<>();

        // Rule 1: CGPA must be at least the minimum (equal passes)
        if (req.cgpa() < req.minCgpa()) {
            reasons.add("CGPA " + req.cgpa() + " is below the required " + req.minCgpa());
        }
        // Rule 2: backlogs must not exceed the limit (equal passes)
        if (req.backlogs() > req.maxBacklogs()) {
            reasons.add("Backlogs " + req.backlogs() + " exceed the limit of " + req.maxBacklogs());
        }
        // Rule 3: the student's department must be in the allowed list
        if (!req.allowedDepartments().contains(req.department())) {
            reasons.add("Department " + req.department() + " is not allowed (allowed: "
                    + String.join(", ", req.allowedDepartments()) + ")");
        }

        // Eligible only when no rule failed
        return new EligibilityResponse(reasons.isEmpty(), reasons);
    }
}
