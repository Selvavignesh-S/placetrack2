package com.placetrack.exception;

/** The eligibility service could not be reached in time (stopped, crashed or too slow). */
public class EligibilityUnavailableException extends RuntimeException {

    public EligibilityUnavailableException(Throwable cause) {
        super("Eligibility check unavailable, try later", cause);
    }
}
