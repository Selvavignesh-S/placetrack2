package com.placetrack.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.placetrack.config.SessionUser;
import com.placetrack.model.Drive;
import com.placetrack.model.Student;
import com.placetrack.service.StudentService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * The logged-in student views and edits their own profile.
 * Note for the report: CGPA and backlogs are self-declared here; a real system would have them verified.
 */
@Controller
@RequestMapping("/student/profile")
public class ProfileController {

    private final StudentService studentService;

    public ProfileController(StudentService studentService) {
        this.studentService = studentService;
    }

    /**
     * Runs before both handlers below: loads the logged-in student from the database.
     * On POST, Spring then copies the allowed form fields onto this loaded object, so
     * email, regNo, password and placed keep their stored values.
     */
    @ModelAttribute("student")
    public Student loadStudent(@SessionAttribute(SessionUser.KEY) SessionUser user) {
        return studentService.getById(user.id());
    }

    /** Only these fields can be changed from the form. */
    @InitBinder("student")
    public void allowOnlyEditableFields(WebDataBinder binder) {
        binder.setAllowedFields("fullName", "department", "cgpa", "backlogs");
    }

    @GetMapping
    public String form(Model model) {
        model.addAttribute("departments", Drive.DEPARTMENTS);
        return "student/profile";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("student") Student student, BindingResult result,
            HttpSession session, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("departments", Drive.DEPARTMENTS);
            return "student/profile";
        }
        studentService.save(student);
        // The navbar shows the name from the session, so store a fresh SessionUser with the new name.
        session.setAttribute(SessionUser.KEY, new SessionUser(student.getId(), student.getFullName(), "STUDENT"));
        ra.addFlashAttribute("message", "Profile saved");
        return "redirect:/student/profile";
    }
}
