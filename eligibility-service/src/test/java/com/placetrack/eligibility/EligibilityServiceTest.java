package com.placetrack.eligibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Plain unit tests of the three rules (no Spring needed). */
class EligibilityServiceTest {

    // No Spring needed: the service is a plain object
    private final EligibilityService service = new EligibilityService();

    // Helper: a drive needing CGPA 7.5, 0 backlogs, IT or CSE
    private EligibilityRequest request(double cgpa, int backlogs, String dept) {
        return new EligibilityRequest(cgpa, backlogs, dept, 7.5, 0, List.of("IT", "CSE"));
    }

    // Equal to the minimum is allowed
    @Test
    void cgpaExactlyEqualPasses() {
        EligibilityResponse res = service.check(request(7.5, 0, "IT"));
        assertTrue(res.eligible());
        assertTrue(res.reasons().isEmpty());
    }

    // Below the minimum gives the CGPA reason
    @Test
    void lowCgpaFails() {
        EligibilityResponse res = service.check(request(7.2, 0, "IT"));
        assertFalse(res.eligible());
        assertEquals(List.of("CGPA 7.2 is below the required 7.5"), res.reasons());
    }

    // More backlogs than allowed gives the backlog reason
    @Test
    void tooManyBacklogsFails() {
        EligibilityResponse res = service.check(request(8.0, 1, "CSE"));
        assertEquals(List.of("Backlogs 1 exceed the limit of 0"), res.reasons());
    }

    // A department not in the list fails
    @Test
    void wrongDepartmentFails() {
        EligibilityResponse res = service.check(request(9.0, 0, "MECH"));
        assertFalse(res.eligible());
        assertEquals(1, res.reasons().size());
    }

    // Every failed rule is reported, not just the first
    @Test
    void allThreeRulesCanFailTogether() {
        EligibilityResponse res = service.check(request(6.0, 2, "ECE"));
        assertEquals(3, res.reasons().size());
    }
}
