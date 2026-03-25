package com.example.grocery_billing.controller;

import com.example.grocery_billing.service
        .SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support
        .RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
@Slf4j
public class SettingsController {

    private final SystemSettingService settingService;

    // ── SHOW SETTINGS PAGE ────────────────────────────
    @GetMapping
    public String showSettings(Model model) {
        model.addAttribute("settings",
                settingService.getAllAsMap());
        model.addAttribute("activePage", "settings");
        model.addAttribute("pageTitle",  "Settings");
        return "settings/index";
    }

    // ── SAVE SETTINGS ─────────────────────────────────
    @PostMapping
    public String saveSettings(
            @RequestParam Map<String, String> params,
            RedirectAttributes ra) {

        try {
            // Remove Spring internal params
            params.remove("_csrf");

            // Only save known setting keys
            Map<String, String> toSave = new HashMap<>();
            for (Map.Entry<String, String> e
                    : params.entrySet()) {
                if (e.getKey().startsWith("shop.")
                        || e.getKey().startsWith(
                        "invoice.")) {
                    toSave.put(e.getKey(),
                            e.getValue() != null
                                    ? e.getValue().trim() : "");
                }
            }

            settingService.saveAll(toSave);

            ra.addFlashAttribute("successMessage",
                    "Settings saved successfully! "
                            + "Changes take effect immediately.");

        } catch (Exception e) {
            log.error("Settings save error", e);
            ra.addFlashAttribute("errorMessage",
                    "Error: " + e.getMessage());
        }

        return "redirect:/settings";
    }
}

