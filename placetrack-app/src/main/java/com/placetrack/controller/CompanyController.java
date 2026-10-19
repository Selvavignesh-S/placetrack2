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

import com.placetrack.model.Company;
import com.placetrack.service.CompanyService;

import jakarta.validation.Valid;

/** Coordinator pages to list, add, edit and delete companies. Every POST ends in a redirect (Post-Redirect-Get). */
@Controller
@RequestMapping("/coordinator/companies")
public class CompanyController {

    private final CompanyService companyService;

    // Constructor injection: Spring passes in the beans this class needs
    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    // GET /coordinator/companies: list page
    @GetMapping
    public String list(Model model) {
        model.addAttribute("companies", companyService.findAll());
        return "coordinator/companies";
    }

    /** from=drive when opened from the drive form's "Add a new company" link. */
    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) String from, Model model) {
        model.addAttribute("company", new Company());
        model.addAttribute("from", from);
        return "coordinator/company-form";
    }

    // GET: form filled with an existing company
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("company", companyService.getById(id));
        return "coordinator/company-form";
    }

    /**
     * Handles both create and update: the form sends a hidden id when editing.
     * from=drive means the user came from the drive form, so go back there with the new company selected.
     */
    @PostMapping
    public String save(@Valid @ModelAttribute Company company, BindingResult result,
            @RequestParam(required = false) String from, Model model, RedirectAttributes ra) {
        model.addAttribute("from", from); // kept if the form is shown again with errors
        // Validation failed: show the form again with the red messages
        if (result.hasErrors()) {
            return "coordinator/company-form";
        }
        Company saved;
        try {
            saved = companyService.save(company);
        } catch (IllegalArgumentException e) {
            result.rejectValue("name", "duplicate", e.getMessage());
            return "coordinator/company-form";
        }
        ra.addFlashAttribute("message", "Company saved");
        if ("drive".equals(from)) {
            return "redirect:/coordinator/drives/new?companyId=" + saved.getId();
        }
        return "redirect:/coordinator/companies";
    }

    // POST: delete, or show why it is not allowed
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            companyService.delete(id);
            ra.addFlashAttribute("message", "Company deleted");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/coordinator/companies";
    }
}
