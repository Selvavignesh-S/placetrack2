package com.placetrack.eligibility;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

/** The three eligibility rules: minimum CGPA, maximum backlogs, allowed department. */
@Service
public class EligibilityService {

    public EligibilityResponse check(EligibilityRequest req) {
        List<String> reasons = new ArrayList<>();

        if (req.cgpa() < req.minCgpa()) {
            reasons.add("CGPA " + req.cgpa() + " is below the required " + req.minCgpa());
        }
        if (req.backlogs() > req.maxBacklogs()) {
            reasons.add("Backlogs " + req.backlogs() + " exceed the limit of " + req.maxBacklogs());
        }
        if (!req.allowedDepartments().contains(req.department())) {
            reasons.add("Department " + req.department() + " is not allowed (allowed: "
                    + String.join(", ", req.allowedDepartments()) + ")");
        }

        return new EligibilityResponse(reasons.isEmpty(), reasons);
    }
}
