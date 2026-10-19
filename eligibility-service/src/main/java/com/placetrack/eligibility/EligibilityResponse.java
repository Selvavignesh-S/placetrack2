package com.placetrack.eligibility;

import java.util.List;

/** JSON reply: eligible or not, plus one human-readable reason per failed rule. */
// Spring (Jackson) turns this into JSON: {"eligible": false, "reasons": [...]}
public record EligibilityResponse(boolean eligible, List<String> reasons) {
}
