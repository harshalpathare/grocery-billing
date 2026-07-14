package com.example.grocery_billing.controller;

import com.example.grocery_billing.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final ShopService shopService;

    // ── LOGIN PAGE ────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {

        if (error != null) {
            String errorMessage = "Invalid username or password. Please try again.";
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            if (session != null) {
                org.springframework.security.core.AuthenticationException ex = 
                        (org.springframework.security.core.AuthenticationException) session.getAttribute(org.springframework.security.web.WebAttributes.AUTHENTICATION_EXCEPTION);
                if (ex != null && ex.getMessage() != null) {
                    errorMessage = ex.getMessage();
                }
            }
            model.addAttribute("errorMessage", errorMessage);
        }

        if (logout != null) model.addAttribute("logoutMessage",
                "You have been logged out successfully.");

        // Show shop name on login page if available (for branding)
        shopService.getCurrentShop().ifPresent(s ->
                model.addAttribute("shopName", s.getShopName()));

        return "auth/login";
    }
}
