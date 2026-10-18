package com.placetrack.api;

import jakarta.validation.constraints.NotNull;

/** JSON body of POST /api/applications: {"studentId": 3, "driveId": 2}. */
public record ApplyRequest(@NotNull Long studentId, @NotNull Long driveId) {
}
