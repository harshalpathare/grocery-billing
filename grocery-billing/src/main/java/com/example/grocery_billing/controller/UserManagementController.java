package com.example.grocery_billing.controller;

import com.example.grocery_billing.entity.User;
import com.example.grocery_billing.repository.UserRepository;
import com.example.grocery_billing.repository.LoginHistoryRepository;
import com.example.grocery_billing.repository.ActivityLogRepository;
import com.example.grocery_billing.service.ActivityLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/settings/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    @GetMapping
    public String userList(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("activePage", "settings");
        model.addAttribute("pageTitle", "User Management");
        model.addAttribute("userForm", new User());
        return "settings/users";
    }

    @PostMapping
    public String saveUser(
            @ModelAttribute("userForm") @Valid User userForm,
            BindingResult result,
            Model model,
            RedirectAttributes ra,
            HttpServletRequest request) {

        if (result.hasErrors()) {
            model.addAttribute("users", userRepository.findAll());
            model.addAttribute("activePage", "settings");
            model.addAttribute("pageTitle", "User Management");
            ra.addFlashAttribute("errorMessage", "Validation failed. Please fill required fields.");
            return "settings/users";
        }

        if (userForm.getId() == null) {
            if (userRepository.existsByUsername(userForm.getUsername())) {
                model.addAttribute("users", userRepository.findAll());
                model.addAttribute("activePage", "settings");
                model.addAttribute("pageTitle", "User Management");
                model.addAttribute("userForm", new User());
                ra.addFlashAttribute("errorMessage", "Username already exists.");
                return "settings/users";
            }
            userForm.setPassword(passwordEncoder.encode(userForm.getPassword()));
            userRepository.save(userForm);
            activityLogService.log(
                    "CREATE", "USER", userForm.getId(),
                    "Created user " + userForm.getUsername(),
                    request);
            ra.addFlashAttribute("successMessage", "User created successfully.");
        } else {
            User existing = userRepository.findById(userForm.getId())
                    .orElse(null);
            if (existing == null) {
                ra.addFlashAttribute("errorMessage", "User not found.");
                return "redirect:/settings/users";
            }
            existing.setFullName(userForm.getFullName());
            existing.setRole(userForm.getRole());
            existing.setEnabled(userForm.getEnabled());
            if (userForm.getPassword() != null && !userForm.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(userForm.getPassword()));
            }
            userRepository.save(existing);
            activityLogService.log(
                    "UPDATE", "USER", existing.getId(),
                    "Updated user " + existing.getUsername(),
                    request);
            ra.addFlashAttribute("successMessage", "User updated successfully.");
        }

        return "redirect:/settings/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             RedirectAttributes ra,
                             HttpServletRequest request) {

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("errorMessage", "User not found.");
            return "redirect:/settings/users";
        }

        try {
            // ── Delete all login history records for this user first ──
            loginHistoryRepository.deleteByUserId(id);
            
            // ── Delete all activity log records for this user ──
            activityLogRepository.deleteByUserId(id);
            
            // ── Now delete the user ──
            userRepository.delete(user);
            
            activityLogService.log(
                    "DELETE", "USER", id,
                    "Deleted user " + user.getUsername(),
                    request);
            ra.addFlashAttribute("successMessage", "User deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error deleting user: " + e.getMessage());
        }
        
        return "redirect:/settings/users";
    }
}