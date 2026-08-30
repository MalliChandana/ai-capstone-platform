package com.capstone.aicapstone;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // =========================
    // HOME PAGE
    // =========================
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // =========================
    // ROLE SELECTION / GET STARTED
    // =========================
    @GetMapping({"/get-started", "/role-selection", "/register"})
    public String getStarted() {
        return "get-started";
    }

    // =========================
    // LOGIN SHORTCUT
    // =========================
    @GetMapping("/login")
    public String login() {
        return "redirect:/student/login";
    }
}
