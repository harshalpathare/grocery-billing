package com.example.grocery_billing.controller;

import com.example.grocery_billing.service.ActivityLogService;
import com.example.grocery_billing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/activity")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;

    // ── Main activity log page ────────────────────
    @GetMapping
    public String activityPage(
            @RequestParam(required = false) Long userId,
            Model model) {

        if (userId != null) {
            model.addAttribute("logs",
                    activityLogService.getLogsByUser(userId));
            model.addAttribute("selectedUserId", userId);
        } else {
            model.addAttribute("logs",
                    activityLogService.getRecentLogs());
        }

        model.addAttribute("loginHistory",
                activityLogService.getRecentLogins());
        model.addAttribute("performance",
                activityLogService.getTodayPerformance());
        model.addAttribute("users",
                userRepository.findAll());
        model.addAttribute("activePage", "activity");
        model.addAttribute("pageTitle",  "Activity Log");
        return "activity/index";
    }
}