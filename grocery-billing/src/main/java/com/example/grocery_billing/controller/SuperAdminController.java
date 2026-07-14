package com.example.grocery_billing.controller;

import com.example.grocery_billing.entity.Shop;
import com.example.grocery_billing.entity.User;
import com.example.grocery_billing.repository.UserRepository;
import com.example.grocery_billing.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SUPER ADMIN CONTROLLER
 * Only accessible to ROLE_SUPER_ADMIN.
 * Manages all shops on the platform.
 */
@Controller
@RequestMapping("/super")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class SuperAdminController {

    private final ShopService    shopService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.example.grocery_billing.service.ShopFeatureService shopFeatureService;

    // ── LIST ALL SHOPS ────────────────────────────────────
    @GetMapping("/shops")
    public String listShops(Model model) {
        List<Shop> shops = shopService.getAllShops();
        model.addAttribute("shops",     shops);
        model.addAttribute("pageTitle", "All Shops — Super Admin");
        model.addAttribute("activePage","super");
        return "super/shops";
    }

    // ── ADD SHOP FORM ─────────────────────────────────────
    @GetMapping("/shops/new")
    public String newShopForm(Model model) {
        model.addAttribute("shop",      new Shop());
        model.addAttribute("pageTitle", "Add New Shop");
        model.addAttribute("isEdit",    false);
        return "super/shop-form";
    }

    // ── SAVE NEW SHOP + CREATE OWNER ACCOUNT ─────────────
    @PostMapping("/shops/new")
    public String saveShop(
            @ModelAttribute Shop shop,
            @RequestParam String ownerUsername,
            @RequestParam String ownerPassword,
            @RequestParam String ownerFullName,
            @RequestParam(required = false) Integer subscriptionMonths,
            RedirectAttributes ra) {
        try {
            // Check username availability
            if (userRepository.existsByUsername(ownerUsername)) {
                ra.addFlashAttribute("errorMessage",
                        "Username '" + ownerUsername + "' is already taken.");
                return "redirect:/super/shops/new";
            }

            // Set subscription
            int months = (subscriptionMonths != null && subscriptionMonths > 0)
                    ? subscriptionMonths : 12;
            shop.setSubscriptionEndDate(LocalDate.now().plusMonths(months));
            shop.setLicenseKey(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
            shop.setActive(true);

            Shop savedShop = shopService.saveShop(shop);

            // Create OWNER user for this shop
            User owner = User.builder()
                    .username(ownerUsername)
                    .fullName(ownerFullName)
                    .password(passwordEncoder.encode(ownerPassword))
                    .role("ROLE_OWNER")
                    .shop(savedShop)
                    .enabled(true)
                    .build();
            userRepository.save(owner);

            log.info("New shop created: {} | Owner: {}", savedShop.getShopName(), ownerUsername);
            ra.addFlashAttribute("successMessage",
                    "Shop '" + savedShop.getShopName() + "' created! "
                    + "Owner login: " + ownerUsername);
        } catch (Exception e) {
            log.error("Error creating shop", e);
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/super/shops";
    }

    // ── EDIT SHOP ─────────────────────────────────────────
    @GetMapping("/shops/{id}/edit")
    public String editShopForm(@PathVariable Long id, Model model) {
        model.addAttribute("shop",      shopService.getShopById(id));
        model.addAttribute("pageTitle", "Edit Shop");
        model.addAttribute("isEdit",    true);
        return "super/shop-form";
    }

    @PostMapping("/shops/{id}/edit")
    public String updateShop(
            @PathVariable Long id,
            @ModelAttribute Shop shopForm,
            RedirectAttributes ra) {
        try {
            Shop existing = shopService.getShopById(id);
            existing.setShopName(shopForm.getShopName());
            existing.setOwnerName(shopForm.getOwnerName());
            existing.setPhone(shopForm.getPhone());
            existing.setEmail(shopForm.getEmail());
            existing.setAddress(shopForm.getAddress());
            existing.setGstin(shopForm.getGstin());
            existing.setBusinessType(shopForm.getBusinessType());
            if (shopForm.getSubscriptionEndDate() != null)
                existing.setSubscriptionEndDate(shopForm.getSubscriptionEndDate());
            shopService.saveShop(existing);
            ra.addFlashAttribute("successMessage", "Shop updated successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/super/shops";
    }

    // ── TOGGLE SHOP ACTIVE ────────────────────────────────
    @PostMapping("/shops/{id}/toggle")
    public String toggleShop(@PathVariable Long id, RedirectAttributes ra) {
        try {
            shopService.toggleShopActive(id);
            ra.addFlashAttribute("successMessage", "Shop status updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/super/shops";
    }

    // ── EXTEND SUBSCRIPTION ───────────────────────────────
    @PostMapping("/shops/{id}/extend")
    public String extendSubscription(
            @PathVariable Long id,
            @RequestParam int months,
            RedirectAttributes ra) {
        try {
            Shop shop = shopService.getShopById(id);
            LocalDate current = shop.getSubscriptionEndDate() != null
                    ? shop.getSubscriptionEndDate() : LocalDate.now();
            shop.setSubscriptionEndDate(current.plusMonths(months));
            shopService.saveShop(shop);
            ra.addFlashAttribute("successMessage",
                    "Subscription updated by " + months + " month(s).");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/super/shops";
    }

    // ── VIEW SHOP USERS ───────────────────────────────────
    @GetMapping("/shops/{id}/users")
    public String shopUsers(@PathVariable Long id, Model model) {
        Shop shop = shopService.getShopById(id);
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getShop() != null
                          && u.getShop().getId().equals(id))
                .toList();
        model.addAttribute("shop",      shop);
        model.addAttribute("users",     users);
        model.addAttribute("pageTitle", "Users — " + shop.getShopName());
        return "super/shop-users";
    }

    // ── ADD USER TO SHOP ──────────────────────────────────
    @PostMapping("/shops/{id}/users/new")
    public String addShopUser(
            @PathVariable Long id,
            @RequestParam String fullName,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String role,
            RedirectAttributes ra) {
        try {
            if (userRepository.existsByUsername(username)) {
                ra.addFlashAttribute("errorMessage", "Username '" + username + "' is already taken.");
                return "redirect:/super/shops/" + id + "/users";
            }
            Shop shop = shopService.getShopById(id);
            User newUser = User.builder()
                    .fullName(fullName)
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .role(role)
                    .shop(shop)
                    .enabled(true)
                    .build();
            userRepository.save(newUser);
            ra.addFlashAttribute("successMessage", "User '" + username + "' created successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/super/shops/" + id + "/users";
    }

    // ── FEATURE TOGGLES ───────────────────────────────────
    @GetMapping("/shops/{id}/features")
    public String shopFeatures(@PathVariable Long id, Model model) {
        Shop shop = shopService.getShopById(id);
        java.util.Map<String, Boolean> features = shopFeatureService.getFeaturesForShop(id);
        
        model.addAttribute("shop", shop);
        model.addAttribute("features", features);
        model.addAttribute("pageTitle", "Features — " + shop.getShopName());
        return "super/shop-features";
    }

    @PostMapping("/shops/{id}/features")
    public String saveShopFeatures(
            @PathVariable Long id,
            @RequestParam(required = false) String enable_credit,
            @RequestParam(required = false) String enable_gst,
            @RequestParam(required = false) String enable_inventory,
            @RequestParam(required = false) String enable_export,
            @RequestParam(required = false) String enable_sms,
            @RequestParam(required = false) String enable_cash_flow,
            @RequestParam(required = false) String enable_loyalty,
            @RequestParam(required = false) String enable_accounting,
            @RequestParam(required = false) String enable_expenses,
            RedirectAttributes ra) {

        try {
            java.util.Map<String, Boolean> updates = new java.util.HashMap<>();
            updates.put("enable_credit", enable_credit != null);
            updates.put("enable_gst", enable_gst != null);
            updates.put("enable_inventory", enable_inventory != null);
            updates.put("enable_export", enable_export != null);
            updates.put("enable_sms", enable_sms != null);
            updates.put("enable_cash_flow", enable_cash_flow != null);
            updates.put("enable_loyalty", enable_loyalty != null);
            updates.put("enable_accounting", enable_accounting != null);
            updates.put("enable_expenses", enable_expenses != null);

            shopFeatureService.saveFeaturesForShop(id, updates);
            ra.addFlashAttribute("successMessage", "Features updated successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
        }
        return "redirect:/super/shops/" + id + "/features";
    }

    @GetMapping("/debug/features")
    @ResponseBody
    public String debugFeatures(
            @org.springframework.beans.factory.annotation.Autowired com.example.grocery_billing.repository.ShopFeatureRepository repo) {
        StringBuilder sb = new StringBuilder("All Features in DB:\n");
        for (com.example.grocery_billing.entity.ShopFeature f : repo.findAll()) {
            sb.append(String.format("ShopID=%d, Feature=%s, Enabled=%s\n", 
                    f.getShop().getId(), f.getFeatureName(), f.getIsEnabled()));
        }
        return sb.toString();
    }
}
