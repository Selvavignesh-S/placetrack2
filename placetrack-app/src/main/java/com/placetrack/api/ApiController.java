package com.placetrack.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.placetrack.model.Application;
import com.placetrack.model.Company;
import com.placetrack.model.Drive;
import com.placetrack.service.ApplicationService;
import com.placetrack.service.CompanyService;
import com.placetrack.service.DriveService;

import jakarta.validation.Valid;

/**
 * REST API: returns JSON instead of HTML pages. Test it with Postman.
 * Errors are turned into JSON by ApiErrorHandler.
 *
 * shortcut: the API has no login and trusts studentId from the body, to keep Postman testing simple;
 * a real system would take the student from the session or a token (JWT).
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final DriveService driveService;
    private final CompanyService companyService;
    private final ApplicationService applicationService;

    // Constructor injection: Spring passes in the beans this class needs
    public ApiController(DriveService driveService, CompanyService companyService,
            ApplicationService applicationService) {
        this.driveService = driveService;
        this.companyService = companyService;
        this.applicationService = applicationService;
    }

    /** GET /api/drives: drives whose deadline is today or later. */
    @GetMapping("/drives")
    public List<Drive> openDrives() {
        return driveService.findOpenDrives();
    }

    /** GET /api/drives/2: one drive, or 404. */
    @GetMapping("/drives/{id}")
    public Drive drive(@PathVariable Long id) {
        return driveService.getById(id);
    }

    /** POST /api/companies {"name": "...", "sector": "..."}: 201 Created with the saved company. */
    @PostMapping("/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public Company createCompany(@Valid @RequestBody Company company) {
        company.setId(null); // POST always creates: an "id" in the body must not overwrite an existing company
        return companyService.save(company);
    }

    /** POST /api/applications {"studentId": 3, "driveId": 2}: 201, or 400 with the reasons, or 503. */
    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public Application apply(@Valid @RequestBody ApplyRequest request) {
        return applicationService.apply(request.studentId(), request.driveId());
    }

    /** GET /api/students/3/applications: one student's applications, newest first. */
    @GetMapping("/students/{id}/applications")
    public List<Application> studentApplications(@PathVariable Long id) {
        return applicationService.myApplications(id);
    }
}
