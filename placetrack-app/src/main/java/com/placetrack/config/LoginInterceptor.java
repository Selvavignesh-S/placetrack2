package com.placetrack.config;

import java.io.IOException;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Runs before every /student/** and /coordinator/** request (registered in AppConfig).
 * Not logged in: go to the login page. Wrong role: 403 Forbidden.
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        HttpSession session = request.getSession(false);
        SessionUser user = session == null ? null : (SessionUser) session.getAttribute(SessionUser.KEY);

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false; // stop: the controller is not called
        }
        String neededRole = request.getServletPath().startsWith("/student/") ? "STUDENT" : "COORDINATOR";
        if (!neededRole.equals(user.role())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true; // continue to the controller
    }
}
