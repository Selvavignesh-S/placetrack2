package com.placetrack.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.placetrack.exception.ApplyException;
import com.placetrack.exception.EligibilityUnavailableException;

/**
 * Turns exceptions thrown by ApiController into JSON error replies with the right HTTP status.
 * Applies only to ApiController; the HTML pages keep their own error page.
 */
@RestControllerAdvice(assignableTypes = ApiController.class)
public class ApiErrorHandler {

    /** A business rule refused the apply: 400 with the message and the eligibility reasons. */
    @ExceptionHandler(ApplyException.class)
    public ResponseEntity<Map<String, Object>> applyRefused(ApplyException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "reasons", e.getReasons()));
    }

    /** Duplicate company name and similar rule violations from M1's services. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    /** @Valid failed on the JSON body: 400 with one message per bad field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalidBody(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.put(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "fields", fields));
    }

    /** Not found (404) and other errors that already carry a status. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> withStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", String.valueOf(e.getReason())));
    }

    /** The eligibility microservice is down or too slow: 503 Service Unavailable. */
    @ExceptionHandler(EligibilityUnavailableException.class)
    public ResponseEntity<Map<String, Object>> serviceDown(EligibilityUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
    }
}
