package com.placetrack.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Temporary home page. M2 deletes this and moves "/" into AuthController (redirect by role). */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/coordinator/drives";
    }
}
