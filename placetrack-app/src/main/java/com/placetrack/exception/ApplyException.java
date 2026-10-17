package com.placetrack.exception;

import java.util.List;

/**
 * A business rule refused an application action (deadline passed, already applied, not eligible...).
 * reasons holds the eligibility service's reasons when the student is not eligible, otherwise it is empty.
 * Unchecked (RuntimeException), so @Transactional rolls back automatically.
 */
public class ApplyException extends RuntimeException {

    private final List<String> reasons;

    public ApplyException(String message) {
        this(message, List.of());
    }

    public ApplyException(String message, List<String> reasons) {
        super(message);
        this.reasons = reasons;
    }

    public List<String> getReasons() {
        return reasons;
    }
}
