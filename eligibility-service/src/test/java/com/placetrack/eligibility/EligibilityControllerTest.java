package com.placetrack.eligibility;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Calls the REST endpoint through Spring MVC without starting a real server. */
@WebMvcTest(EligibilityController.class)
@Import(EligibilityService.class)
class EligibilityControllerTest {

    // MockMvc sends fake HTTP requests to the controller
    @Autowired
    private MockMvc mockMvc;

    // Valid JSON: 200 and the two reasons
    @Test
    void validRequestReturnsReasons() throws Exception {
        String body = """
                { "cgpa": 7.2, "backlogs": 1, "department": "IT",
                  "minCgpa": 7.5, "maxBacklogs": 0, "allowedDepartments": ["IT", "CSE"] }
                """;
        mockMvc.perform(post("/eligibility/check").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.reasons.length()").value(2));
    }

    // cgpa missing: @Valid rejects it with 400 Bad Request
    @Test
    void missingFieldReturns400() throws Exception {
        String body = """
                { "backlogs": 0, "department": "IT",
                  "minCgpa": 7.5, "maxBacklogs": 0, "allowedDepartments": ["IT"] }
                """;
        mockMvc.perform(post("/eligibility/check").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
