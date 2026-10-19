package com.placetrack.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.placetrack.exception.ApplyException;
import com.placetrack.model.ApplicationStatus;
import com.placetrack.service.ApplicationService;
import com.placetrack.service.DriveService;

/** Coordinator pages: see who applied to a drive and shortlist, select or reject them. */
@Controller
public class ApplicantController {

    private final DriveService driveService;
    private final ApplicationService applicationService;

    // Constructor injection: Spring passes in the beans this class needs
    public ApplicantController(DriveService driveService, ApplicationService applicationService) {
        this.driveService = driveService;
        this.applicationService = applicationService;
    }

    // GET: applicants page of one drive
    @GetMapping("/coordinator/drives/{id}/applicants")
    public String applicants(@PathVariable Long id, Model model) {
        model.addAttribute("drive", driveService.getById(id));
        model.addAttribute("applications", applicationService.applicantsOfDrive(id));
        return "coordinator/applicants";
    }

    /** status arrives as text ("SHORTLISTED") and Spring converts it to the enum. driveId is only for the redirect. */
    @PostMapping("/coordinator/applications/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam ApplicationStatus status,
            @RequestParam Long driveId, RedirectAttributes ra) {
        try {
            applicationService.updateStatus(id, status);
            ra.addFlashAttribute("message", "Status changed to " + status);
        } catch (ApplyException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/coordinator/drives/" + driveId + "/applicants";
    }
}
