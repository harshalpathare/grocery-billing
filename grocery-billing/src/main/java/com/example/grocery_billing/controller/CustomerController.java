package com.example.grocery_billing.controller;


import com.example.grocery_billing.entity.Customer;
import com.example.grocery_billing.entity.Transaction;
import com.example.grocery_billing.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * CUSTOMER CONTROLLER
 *
 * URL map:
 *   GET  /customers          → list all customers
 *   GET  /customers/new      → show add form
 *   POST /customers/new      → save new customer
 *   GET  /customers/{id}     → view customer profile + history
 *   GET  /customers/{id}/edit → show edit form
 *   POST /customers/{id}/edit → save changes
 *   POST /customers/{id}/delete → soft delete
 */
@Controller
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // ─────────────────────────────────────────────────────
    // LIST ALL CUSTOMERS
    // ─────────────────────────────────────────────────────
    @GetMapping
    public String listCustomers(Model model) {
        List<Customer> customers =
                customerService.getAllActiveCustomers();

        // ✅ Pre-calculate IDs with dues — no T() needed in HTML
        java.util.Set<Long> customersWithDues = customers.stream()
                .filter(c -> c.getBalance() != null
                        && c.getBalance().compareTo(
                        java.math.BigDecimal.ZERO) > 0)
                .map(Customer::getId)
                .collect(java.util.stream.Collectors.toSet());

        // Stats for top cards
        long totalWithDues = customersWithDues.size();
        java.math.BigDecimal totalPending = customers.stream()
                .filter(c -> c.getBalance() != null
                        && c.getBalance().compareTo(
                        java.math.BigDecimal.ZERO) > 0)
                .map(Customer::getBalance)
                .reduce(java.math.BigDecimal.ZERO,
                        java.math.BigDecimal::add);

        model.addAttribute("customers",         customers);
        model.addAttribute("customersWithDues", customersWithDues);
        model.addAttribute("totalWithDues",     totalWithDues);
        model.addAttribute("totalPending",      totalPending);
        model.addAttribute("activePage",        "customers");
        model.addAttribute("pageTitle",         "All Customers");
        return "customer/list";
    }
    // ─────────────────────────────────────────────────────
    // VIEW CUSTOMER PROFILE
    // ─────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public String viewCustomer(@PathVariable("id") Long id, Model model) {
        Customer customer = customerService.getCustomerById(id);
        List<Transaction> transactions =
                customerService.getTransactionHistory(id);

        model.addAttribute("customer", customer);
        model.addAttribute("transactions", transactions);
        model.addAttribute("activePage", "customers");
        model.addAttribute("pageTitle", customer.getName() + " — Profile");

        return "customer/view";
    }

    // ─────────────────────────────────────────────────────
    // SHOW ADD FORM
    // ─────────────────────────────────────────────────────
    @GetMapping("/new")
    public String showAddForm(Model model) {
        model.addAttribute("customer", new Customer());
        model.addAttribute("activePage", "customers");
        model.addAttribute("pageTitle", "Add Customer");
        model.addAttribute("isEdit", false);
        return "customer/form";
    }

    // ─────────────────────────────────────────────────────
    // SAVE NEW CUSTOMER
    // ─────────────────────────────────────────────────────
    @PostMapping("/new")
    public String saveCustomer(
            @Valid @ModelAttribute("customer") Customer customer,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "customers");
            model.addAttribute("pageTitle", "Add Customer");
            model.addAttribute("isEdit", false);
            return "customer/form";
        }

        try {
            Customer saved = customerService.saveCustomer(customer);

            // Check if this was a reactivation (had old credit history)
            boolean wasReactivated =
                    saved.getTotalCredit().compareTo(java.math.BigDecimal.ZERO) > 0
                            || saved.getTotalPaid().compareTo(java.math.BigDecimal.ZERO) > 0;

            if (wasReactivated) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Customer '" + saved.getName() + "' was previously deleted. "
                                + "Account reactivated! Previous credit history is preserved.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Customer '" + saved.getName() + "' added successfully!");
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/customers/new";
        }

        return "redirect:/customers";
    }



    // ─────────────────────────────────────────────────────
    // SHOW EDIT FORM
    // ─────────────────────────────────────────────────────
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("customer", customerService.getCustomerById(id));
        model.addAttribute("activePage", "customers");
        model.addAttribute("pageTitle", "Edit Customer");
        model.addAttribute("isEdit", true);
        return "customer/form";
    }

    // ─────────────────────────────────────────────────────
    // SAVE EDITED CUSTOMER
    // ─────────────────────────────────────────────────────
    @PostMapping("/{id}/edit")
    public String updateCustomer(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("customer") Customer customer,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "customers");
            model.addAttribute("pageTitle", "Edit Customer");
            model.addAttribute("isEdit", true);
            return "customer/form";
        }

        try {
            customer.setId(id);
            customerService.saveCustomer(customer);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Customer updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/customers";
    }

    // ─────────────────────────────────────────────────────
    // DELETE CUSTOMER
    // ─────────────────────────────────────────────────────
    @PostMapping("/{id}/delete")
    public String deleteCustomer(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {
        try {
            customerService.deleteCustomer(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Customer removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/customers";
    }
}

