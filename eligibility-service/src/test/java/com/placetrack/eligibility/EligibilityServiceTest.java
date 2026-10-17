package com.placetrack.eligibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Plain unit tests of the three rules (no Spring needed). */
class EligibilityServiceTest {

    private final EligibilityService service = new EligibilityService();

    private EligibilityRequest request(double cgpa, int backlogs, String dept) {
        return new EligibilityRequest(cgpa, backlogs, dept, 7.5, 0, List.of("IT", "CSE"));
    }

    @Test
    void cgpaExactlyEqualPasses() {
        EligibilityResponse res = service.check(request(7.5, 0, "IT"));
        assertTrue(res.eligible());
        assertTrue(res.reasons().isEmpty());
    }

    @Test
    void lowCgpaFails() {
        EligibilityResponse res = service.check(request(7.2, 0, "IT"));
        assertFalse(res.eligible());
        assertEquals(List.of("CGPA 7.2 is below the required 7.5"), res.reasons());
    }

    @Test
    void tooManyBacklogsFails() {
        EligibilityResponse res = service.check(request(8.0, 1, "CSE"));
        assertEquals(List.of("Backlogs 1 exceed the limit of 0"), res.reasons());
    }

    @Test
    void wrongDepartmentFails() {
        EligibilityResponse res = service.check(request(9.0, 0, "MECH"));
        assertFalse(res.eligible());
        assertEquals(1, res.reasons().size());
    }

    @Test
    void allThreeRulesCanFailTogether() {
        EligibilityResponse res = service.check(request(6.0, 2, "ECE"));
        assertEquals(3, res.reasons().size());
    }
}
