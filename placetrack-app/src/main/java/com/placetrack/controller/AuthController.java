package com.placetrack.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.placetrack.config.SessionUser;
import com.placetrack.model.Drive;
import com.placetrack.model.Student;
import com.placetrack.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/** Home page redirect, login, logout and student registration. */
@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Sends each user to their own start page. */
    @GetMapping("/")
    public String home(@SessionAttribute(name = SessionUser.KEY, required = false) SessionUser user) {
        if (user == null) {
            return "redirect:/login";
        }
        return user.isStudent() ? "redirect:/student/drives" : "redirect:/coordinator/dashboard";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
            HttpServletRequest request, RedirectAttributes ra) {
        Optional<SessionUser> user = authService.login(email, password);
        if (user.isEmpty()) {
            ra.addFlashAttribute("error", "Wrong email or password");
            return "redirect:/login";
        }
        HttpSession session = request.getSession();
        request.changeSessionId(); // new session id after login, so an id stolen before login is useless
        session.setAttribute(SessionUser.KEY, user.get());
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    /** Only these form fields may be bound, so nobody can post id, placed or password to the Student. */
    @InitBinder("student")
    public void allowOnlyFormFields(WebDataBinder binder) {
        binder.setAllowedFields("fullName", "email", "regNo", "department", "cgpa", "backlogs");
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("student", new Student());
        model.addAttribute("departments", Drive.DEPARTMENTS);
        return "register";
    }

    /** The password arrives separately as rawPassword, because Student.password holds the hash. */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute Student student, BindingResult result,
            @RequestParam String rawPassword, Model model, RedirectAttributes ra) {
        if (!result.hasErrors()) {
            try {
                authService.register(student, rawPassword);
                ra.addFlashAttribute("message", "Registered. Please log in.");
                return "redirect:/login";
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        model.addAttribute("departments", Drive.DEPARTMENTS);
        return "register";
    }
}
