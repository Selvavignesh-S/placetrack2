package com.placetrack.controller;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.placetrack.config.SessionUser;
import com.placetrack.exception.ApplyException;
import com.placetrack.exception.EligibilityUnavailableException;
import com.placetrack.service.ApplicationService;
import com.placetrack.service.DriveService;

/**
 * Student pages: open drives, drive detail with "Check eligibility" and "Apply", my applications, withdraw.
 * LoginInterceptor guarantees a logged-in STUDENT, so the session user is always present here.
 */
@Controller
@RequestMapping("/student")
public class StudentController {

    private final DriveService driveService;
    private final ApplicationService applicationService;

    public StudentController(DriveService driveService, ApplicationService applicationService) {
        this.driveService = driveService;
        this.applicationService = applicationService;
    }

    @GetMapping("/drives")
    public String drives(@SessionAttribute(SessionUser.KEY) SessionUser user, Model model) {
        Set<Long> appliedDriveIds = applicationService.myApplications(user.id()).stream()
                .map(a -> a.getDrive().getId())
                .collect(Collectors.toSet());
        model.addAttribute("drives", driveService.findOpenDrives());
        model.addAttribute("appliedDriveIds", appliedDriveIds);
        return "student/drives";
    }

    @GetMapping("/drives/{id}")
    public String driveDetail(@PathVariable Long id, Model model) {
        model.addAttribute("drive", driveService.getById(id));
        return "student/drive-detail";
    }

    /** A GET because it only reads: nothing is saved. Shows the result on the same detail page. */
    @GetMapping("/drives/{id}/check")
    public String check(@PathVariable Long id, @SessionAttribute(SessionUser.KEY) SessionUser user, Model model) {
        model.addAttribute("drive", driveService.getById(id));
        try {
            model.addAttribute("result", applicationService.checkEligibility(user.id(), id));
        } catch (EligibilityUnavailableException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "student/drive-detail";
    }

    @PostMapping("/drives/{id}/apply")
    public String apply(@PathVariable Long id, @SessionAttribute(SessionUser.KEY) SessionUser user,
            RedirectAttributes ra) {
        try {
            applicationService.apply(user.id(), id);
            ra.addFlashAttribute("message", "Applied successfully");
            return "redirect:/student/applications";
        } catch (ApplyException e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("reasons", e.getReasons());
        } catch (EligibilityUnavailableException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/drives/" + id;
    }

    @GetMapping("/applications")
    public String applications(@SessionAttribute(SessionUser.KEY) SessionUser user, Model model) {
        model.addAttribute("applications", applicationService.myApplications(user.id()));
        return "student/applications";
    }

    @PostMapping("/applications/{id}/withdraw")
    public String withdraw(@PathVariable Long id, @SessionAttribute(SessionUser.KEY) SessionUser user,
            RedirectAttributes ra) {
        try {
            applicationService.withdraw(id, user.id());
            ra.addFlashAttribute("message", "Application withdrawn");
        } catch (ApplyException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/applications";
    }
}
