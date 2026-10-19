package com.placetrack.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.placetrack.model.Drive;
import com.placetrack.service.CompanyService;
import com.placetrack.service.DriveService;

import jakarta.validation.Valid;

/** Coordinator pages to list, create, edit and delete placement drives. */
@Controller
@RequestMapping("/coordinator/drives")
public class DriveController {

    private final DriveService driveService;
    private final CompanyService companyService;

    // Constructor injection: Spring passes in the beans this class needs
    public DriveController(DriveService driveService, CompanyService companyService) {
        this.driveService = driveService;
        this.companyService = companyService;
    }

    // GET /coordinator/drives: list page
    @GetMapping
    public String list(Model model) {
        model.addAttribute("drives", driveService.findAll());
        return "coordinator/drives";
    }

    /** companyId is set when coming back from "Add a new company", to preselect it. */
    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long companyId, Model model) {
        Drive drive = new Drive();
        if (companyId != null) {
            drive.setCompany(companyService.getById(companyId));
        }
        return showForm(drive, model);
    }

    // GET: form filled with an existing drive
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return showForm(driveService.getById(id), model);
    }

    /**
     * Handles both create and update. The company comes as a separate companyId parameter;
     * the ticked department checkboxes arrive as several values that Spring joins into "CSE,IT".
     */
    @PostMapping
    public String save(@Valid @ModelAttribute Drive drive, BindingResult result,
            @RequestParam Long companyId, Model model, RedirectAttributes ra) {
        drive.setCompany(companyService.getById(companyId));
        if (result.hasErrors()) {
            return showForm(drive, model);
        }
        driveService.save(drive);
        ra.addFlashAttribute("message", "Drive saved");
        return "redirect:/coordinator/drives";
    }

    // POST: delete, or show why it is not allowed
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            driveService.delete(id);
            ra.addFlashAttribute("message", "Drive deleted");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/coordinator/drives";
    }

    // Puts the drive plus the company and department lists into the model
    private String showForm(Drive drive, Model model) {
        model.addAttribute("drive", drive);
        model.addAttribute("companies", companyService.findAll());
        model.addAttribute("departments", Drive.DEPARTMENTS);
        return "coordinator/drive-form";
    }
}
