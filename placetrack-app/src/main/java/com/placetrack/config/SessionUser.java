package com.placetrack.config;

/**
 * Who is logged in. Stored in the HTTP session under KEY after a successful login.
 * role is "STUDENT" or "COORDINATOR". In templates: ${session.user.name()}.
 */
public record SessionUser(Long id, String name, String role) {

    // Name of the session attribute
    public static final String KEY = "user";

    // Lets templates write ${session.user.student}
    public boolean isStudent() {
        return "STUDENT".equals(role);
    }
}
