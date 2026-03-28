package com.example.grocery_billing.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/error")
public class GlobalErrorController {

    @GetMapping
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            
            if (statusCode == HttpStatus.FORBIDDEN.value()) {
                model.addAttribute("errorCode", 403);
                model.addAttribute("errorMessage", "Access Denied");
                model.addAttribute("errorDescription", 
                        "You do not have permission to access this page. " +
                        "Only administrators can access this resource.");
                return "error/access-denied";
            } else if (statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("errorCode", 404);
                model.addAttribute("errorMessage", "Page Not Found");
                model.addAttribute("errorDescription", 
                        "The page you are looking for does not exist.");
                return "error/not-found";
            }
        }

        model.addAttribute("errorCode", 500);
        model.addAttribute("errorMessage", "Internal Server Error");
        model.addAttribute("errorDescription", message != null ? 
                message.toString() : "An unexpected error occurred.");
        return "error/error";
    }
}
