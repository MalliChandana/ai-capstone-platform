package com.capstone.aicapstone;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.stereotype.Controller;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
@Controller
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleException(Exception ex, Model model) {
        model.addAttribute("statusCode", 500);
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "An unexpected server error occurred.");
        return "error";
    }

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        
        int statusCode = 500;
        if (status != null) {
            try {
                statusCode = Integer.parseInt(status.toString());
            } catch (Exception ignored) {}
        }

        String errorMessage = (message != null && !message.toString().isEmpty()) 
                ? message.toString() 
                : (statusCode == 404 ? "The page or resource you are looking for does not exist." : "An unexpected error occurred. Please try again.");

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorMessage", errorMessage);
        return "error";
    }
}
