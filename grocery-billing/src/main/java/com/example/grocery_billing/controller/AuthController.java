package com.example.grocery_billing.controller;

import com.example.grocery_billing.config.ShopConfig;
import com.example.grocery_billing.entity.User;
import com.example.grocery_billing.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * AUTH CONTROLLER
 *
 * Handles:
 *   GET  /login          → show login page
 *   GET  /settings       → shop settings page
 *   POST /settings       → save shop settings
 *   GET  /users          → user management
 *   POST /users/new      → create user
 *   POST /users/{id}/delete → delete user
 *   POST /users/{id}/change-password → change password
 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository   userRepository;
    private final PasswordEncoder  passwordEncoder;
    private final ShopConfig       shopConfig;

    // ─────────────────────────────────────────────────────
    // LOGIN PAGE
    // Spring Security handles the actual authentication.
    // We just show the page.
    // ─────────────────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage",
                    "Invalid username or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage",
                    "You have been logged out successfully.");
        }

        model.addAttribute("shopName", shopConfig.getName());
        return "auth/login";
    }

    // ─────────────────────────────────────────────────────
    // SHOP SETTINGS PAGE
    // ─────────────────────────────────────────────────────
    /*@GetMapping("/settings")
    public String settingsPage(Model model, Authentication auth) {
        List<User> users = userRepository.findAll();

        model.addAttribute("shopConfig",  shopConfig);
        model.addAttribute("users",       users);
        model.addAttribute("currentUser", auth.getName());
        model.addAttribute("newUser",     new User());
        model.addAttribute("activePage",  "settings");
        model.addAttribute("pageTitle",   "Settings");

        return "auth/settings";
    }*/

    // ─────────────────────────────────────────────────────
    // CREATE NEW USER
    // ─────────────────────────────────────────────────────
    @PostMapping("/users/new")
    public String createUser(
            @RequestParam String username,
            @RequestParam String fullName,
            @RequestParam String password,
            @RequestParam String role,
            RedirectAttributes redirectAttributes) {

        // Check username availability
        if (userRepository.existsByUsername(username)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Username '" + username + "' is already taken.");
            return "redirect:/settings";
        }

        // Password strength check
        if (password.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Password must be at least 6 characters.");
            return "redirect:/settings";
        }

        User user = User.builder()
                .username(username)
                .fullName(fullName)
                .password(passwordEncoder.encode(password))
                .role(role)
                .enabled(true)
                .build();

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("successMessage",
                "User '" + username + "' created successfully!");

        return "redirect:/settings";
    }

    // ─────────────────────────────────────────────────────
    // CHANGE PASSWORD
    // ─────────────────────────────────────────────────────
    @PostMapping("/users/{id}/change-password")
    public String changePassword(
            @PathVariable Long id,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only admin or the user themselves can change password
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isSelf  = auth.getName().equals(user.getUsername());

        if (!isAdmin && !isSelf) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You can only change your own password.");
            return "redirect:/settings";
        }

        // For self-change, verify current password
        if (isSelf && !isAdmin) {
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Current password is incorrect.");
                return "redirect:/settings";
            }
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "New passwords do not match.");
            return "redirect:/settings";
        }

        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Password must be at least 6 characters.");
            return "redirect:/settings";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage",
                "Password changed successfully!");
        return "redirect:/settings";
    }

    // ─────────────────────────────────────────────────────
    // DELETE USER
    // ─────────────────────────────────────────────────────
    @PostMapping("/users/{id}/delete")
    public String deleteUser(
            @PathVariable Long id,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Cannot delete yourself
        if (auth.getName().equals(user.getUsername())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You cannot delete your own account.");
            return "redirect:/settings";
        }

        // Cannot delete the last admin
        long adminCount = userRepository.findAll().stream()
                .filter(u -> "ROLE_ADMIN".equals(u.getRole()) && u.getEnabled())
                .count();

        if ("ROLE_ADMIN".equals(user.getRole()) && adminCount <= 1) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete the last admin account.");
            return "redirect:/settings";
        }

        userRepository.delete(user);
        redirectAttributes.addFlashAttribute("successMessage",
                "User deleted successfully.");
        return "redirect:/settings";
    }

    // ─────────────────────────────────────────────────────
    // TOGGLE USER ENABLE/DISABLE
    // ─────────────────────────────────────────────────────
    @PostMapping("/users/{id}/toggle")
    public String toggleUser(
            @PathVariable Long id,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (auth.getName().equals(user.getUsername())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "You cannot disable your own account.");
            return "redirect:/settings";
        }

        user.setEnabled(!user.getEnabled());
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage",
                "User " + (user.getEnabled() ? "enabled" : "disabled") + ".");
        return "redirect:/settings";
    }
}