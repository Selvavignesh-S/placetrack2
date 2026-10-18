package com.placetrack.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.placetrack.service.DriveService;
import com.placetrack.service.StudentService;

/** Coordinator start page: student and placement counts, and applications per drive. */
@Controller
public class DashboardController {

    private final StudentService studentService;
    private final DriveService driveService;

    public DashboardController(StudentService studentService, DriveService driveService) {
        this.studentService = studentService;
        this.driveService = driveService;
    }

    @GetMapping("/coordinator/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalStudents", studentService.countStudents());
        model.addAttribute("placedStudents", studentService.countPlaced());
        model.addAttribute("openDrives", driveService.findOpenDrives().size());
        model.addAttribute("rows", driveService.applicationCounts());
        return "coordinator/dashboard";
    }
}
