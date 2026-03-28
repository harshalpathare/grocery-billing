package com.example.grocery_billing.controller;

import com.example.grocery_billing.entity.User;
import com.example.grocery_billing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDebugController {

    private final UserRepository userRepository;

    @GetMapping("/users")
    public String listUsersForDebug(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("pageTitle", "Admin Debug - Users");
        return "admin/debug-users";
    }

    @GetMapping("/promote/{id}")
    public String promoteToAdmin(@PathVariable Long id, RedirectAttributes ra) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("errorMessage", "User not found.");
            return "redirect:/admin/users";
        }

        user.setRole("ROLE_ADMIN");
        userRepository.save(user);
        ra.addFlashAttribute("successMessage", "✓ User '" + user.getUsername() + "' promoted to ROLE_ADMIN. Log out and log back in.");
        return "redirect:/admin/users";
    }
}