package com.capstone.aicapstone;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class ProjectController {

    // Aliases to ensure backward compatibility for any old links
    @GetMapping("/company/project/create")
    public String createProjectAlias() {
        return "redirect:/company/project/post";
    }

    @PostMapping("/company/project/create")
    public String createProjectSubmitAlias() {
        return "redirect:/company/project/post";
    }
}