package com.placetrack.eligibility;

import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** JSON body of POST /eligibility/check: the student's marks plus the drive's criteria. */
public record EligibilityRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("10.0") Double cgpa,
        @NotNull @Min(0) Integer backlogs,
        @NotBlank String department,
        @NotNull @DecimalMin("0.0") @DecimalMax("10.0") Double minCgpa,
        @NotNull @Min(0) Integer maxBacklogs,
        @NotEmpty List<String> allowedDepartments) {
}
