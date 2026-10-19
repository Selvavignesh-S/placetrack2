package com.placetrack.exception;

/** The eligibility service could not be reached in time (stopped, crashed or too slow). */
public class EligibilityUnavailableException extends RuntimeException {

    // cause = the original network error, kept for the logs
    public EligibilityUnavailableException(Throwable cause) {
        super("Eligibility check unavailable, try later", cause);
    }
}
