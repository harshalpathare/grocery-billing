package com.example.grocery_billing.controller;

import com.example.grocery_billing.entity.User;
import com.example.grocery_billing.repository.UserRepository;
import com.example.grocery_billing.service.ShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;

@Controller
@RequestMapping("/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;
    private final UserRepository userRepository;

    @PostMapping("/start")
    public String startShift(@RequestParam(value = "openingBalance", defaultValue = "0") BigDecimal openingBalance,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = null;
            if (principal != null) {
                user = userRepository.findByUsername(principal.getName()).orElse(null);
            }
            
            shiftService.startShift(user, openingBalance);
            redirectAttributes.addFlashAttribute("successMessage", "Shift started successfully with ₹" + openingBalance + " in drawer.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error starting shift: " + e.getMessage());
        }
        return "redirect:/";
    }

    @PostMapping("/end")
    public String endShift(@RequestParam(value = "closingBalance", defaultValue = "0") BigDecimal closingBalance,
                           RedirectAttributes redirectAttributes) {
        try {
            shiftService.endShift(closingBalance);
            redirectAttributes.addFlashAttribute("successMessage", "Shift ended successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error ending shift: " + e.getMessage());
        }
        return "redirect:/";
    }
}
