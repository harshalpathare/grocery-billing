package com.example.grocery_billing.controller;



import com.example.grocery_billing.entity.Customer;
import com.example.grocery_billing.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * CREDIT CONTROLLER
 *
 * Handles the credit/udhari management pages.
 *
 * URL map:
 *   GET  /credit              → credit overview (all customers with dues)
 *   GET  /credit/pay/{id}     → show payment form for a customer
 *   POST /credit/pay/{id}     → record a payment
 */
@Controller
@RequestMapping("/credit")
@RequiredArgsConstructor
public class CreditController {

    private final CustomerService customerService;

    // ─────────────────────────────────────────────────────
    // CREDIT OVERVIEW PAGE
    // Shows all customers with pending balance
    // ─────────────────────────────────────────────────────
    @GetMapping
    public String creditOverview(Model model) {
        List<Customer> customersWithDues =
                customerService.getCustomersWithPendingBalance();

        model.addAttribute("customers", customersWithDues);
        model.addAttribute("totalPending",
                customerService.getTotalPendingBalance());
        model.addAttribute("activePage", "credit");
        model.addAttribute("pageTitle", "Credit / Udhari");

        return "credit/overview";
    }

    // ─────────────────────────────────────────────────────
    // SHOW PAYMENT FORM
    // ─────────────────────────────────────────────────────
    @GetMapping("/pay/{customerId}")
    public String showPaymentForm(
            @PathVariable Long customerId, Model model) {
        Customer customer = customerService.getCustomerById(customerId);
        model.addAttribute("customer", customer);
        model.addAttribute("activePage", "credit");
        model.addAttribute("pageTitle", "Record Payment");
        return "credit/pay";
    }

    // ─────────────────────────────────────────────────────
    // PROCESS PAYMENT
    // ─────────────────────────────────────────────────────
    @PostMapping("/pay/{customerId}")
    public String processPayment(
            @PathVariable Long customerId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please enter a valid payment amount.");
            return "redirect:/credit/pay/" + customerId;
        }

        try {
            customerService.recordPayment(customerId, amount, description);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment of ₹" + amount + " recorded successfully!");
            return "redirect:/customers/" + customerId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/credit/pay/" + customerId;
        }
    }
    // ─────────────────────────────────────────────────────
// CLEAR ALL DUES FOR A CUSTOMER
// This records a full payment and zeros their balance
// ─────────────────────────────────────────────────────
    @PostMapping("/clear/{customerId}")
    public String clearDues(
            @PathVariable Long customerId,
            RedirectAttributes redirectAttributes) {

        try {
            Customer customer = customerService
                    .getCustomerById(customerId);

            BigDecimal pendingBalance = customer.getBalance();

            if (pendingBalance == null
                    || pendingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "No pending dues to clear.");
                return "redirect:/credit";
            }

            // Record as a full payment
            customerService.recordPayment(
                    customerId,
                    pendingBalance,
                    "Full dues cleared — ₹" + pendingBalance);

            redirectAttributes.addFlashAttribute("successMessage",
                    "All dues cleared for "
                            + customer.getName()
                            + "! ₹" + pendingBalance + " marked as paid.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error clearing dues: " + e.getMessage());
        }

        return "redirect:/credit";
    }
}