package com.placetrack.client;

import java.util.List;

/**
 * The eligibility service's JSON reply. A copy of its EligibilityResponse on purpose:
 * microservices share only the JSON shape, never Java classes.
 */
public record EligibilityResult(boolean eligible, List<String> reasons) {
}
