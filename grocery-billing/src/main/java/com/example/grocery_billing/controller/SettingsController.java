package com.example.grocery_billing.controller;

import com.example.grocery_billing.entity.Shop;
import com.example.grocery_billing.entity.User;
import com.example.grocery_billing.repository.UserRepository;
import com.example.grocery_billing.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@RequestMapping("/settings")
@PreAuthorize("hasRole('OWNER')")
@RequiredArgsConstructor
@Slf4j
public class SettingsController {

    private final ShopService     shopService;
    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── SHOW SETTINGS PAGE ────────────────────────────────
    @GetMapping
    public String showSettings(Model model, Authentication auth) {
        Shop shop = shopService.getCurrentShop().orElse(new Shop());

        // Users for this shop only (not super admin users)
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getShop() != null
                        && u.getShop().getId().equals(shop.getId()))
                .toList();

        model.addAttribute("shop",        shop);
        model.addAttribute("users",       users);
        model.addAttribute("currentUser", auth != null ? auth.getName() : "");
        model.addAttribute("activePage",  "settings");
        model.addAttribute("pageTitle",   "Settings");
        return "settings/index";
    }

    // ── SAVE SHOP SETTINGS ────────────────────────────────
    @PostMapping
    public String saveSettings(
            @ModelAttribute Shop shopForm,
            RedirectAttributes ra) {
        try {
            shopService.saveSettings(shopForm);
            ra.addFlashAttribute("successMessage",
                    "Settings saved successfully!");
        } catch (Exception e) {
            log.error("Settings save error", e);
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/settings";
    }

    // ── CREATE USER (scoped to current shop) ─────────────
    @PostMapping("/users/new")
    public String createUser(
            @RequestParam String username,
            @RequestParam String fullName,
            @RequestParam String password,
            @RequestParam String role,
            RedirectAttributes ra) {

        if (userRepository.existsByUsername(username)) {
            ra.addFlashAttribute("errorMessage",
                    "Username '" + username + "' is already taken.");
            return "redirect:/settings";
        }
        if (password.length() < 6) {
            ra.addFlashAttribute("errorMessage",
                    "Password must be at least 6 characters.");
            return "redirect:/settings";
        }

        Shop shop = shopService.getCurrentShop().orElse(null);

        User user = User.builder()
                .username(username)
                .fullName(fullName)
                .password(passwordEncoder.encode(password))
                .role(role)
                .shop(shop)
                .enabled(true)
                .build();

        userRepository.save(user);
        ra.addFlashAttribute("successMessage",
                "User '" + username + "' created successfully!");
        return "redirect:/settings";
    }

    // ── CHANGE PASSWORD ───────────────────────────────────
    @PostMapping("/users/{id}/change-password")
    public String changePassword(
            @PathVariable Long id,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Authentication auth,
            RedirectAttributes ra) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isOwner = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OWNER")
                            || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        boolean isSelf = auth.getName().equals(user.getUsername());

        if (!isOwner && !isSelf) {
            ra.addFlashAttribute("errorMessage", "You can only change your own password.");
            return "redirect:/settings";
        }
        if (isSelf && !isOwner) {
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                ra.addFlashAttribute("errorMessage", "Current password is incorrect.");
                return "redirect:/settings";
            }
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("errorMessage", "New passwords do not match.");
            return "redirect:/settings";
        }
        if (newPassword.length() < 6) {
            ra.addFlashAttribute("errorMessage", "Password must be at least 6 characters.");
            return "redirect:/settings";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        ra.addFlashAttribute("successMessage", "Password changed successfully!");
        return "redirect:/settings";
    }

    // ── DELETE USER ───────────────────────────────────────
    @PostMapping("/users/{id}/delete")
    public String deleteUser(
            @PathVariable Long id,
            Authentication auth,
            RedirectAttributes ra) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (auth.getName().equals(user.getUsername())) {
            ra.addFlashAttribute("errorMessage", "You cannot delete your own account.");
            return "redirect:/settings";
        }

        userRepository.delete(user);
        ra.addFlashAttribute("successMessage", "User deleted successfully.");
        return "redirect:/settings";
    }

    // ── TOGGLE USER ───────────────────────────────────────
    @PostMapping("/users/{id}/toggle")
    public String toggleUser(
            @PathVariable Long id,
            Authentication auth,
            RedirectAttributes ra) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (auth.getName().equals(user.getUsername())) {
            ra.addFlashAttribute("errorMessage", "You cannot disable your own account.");
            return "redirect:/settings";
        }

        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
        ra.addFlashAttribute("successMessage",
                "User " + (user.getEnabled() ? "enabled" : "disabled") + ".");
        return "redirect:/settings";
    }
}
