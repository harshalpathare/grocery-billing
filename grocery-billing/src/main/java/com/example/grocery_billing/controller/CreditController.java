package com.example.grocery_billing.controller;

import com.example.grocery_billing.config.ShopConfig;
import com.example.grocery_billing.entity.Customer;
import com.example.grocery_billing.service.CustomerService;
import com.example.grocery_billing.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/credit")
@RequiredArgsConstructor
@Slf4j
public class CreditController {

    private final CustomerService customerService;
    private final QrCodeService   qrCodeService;  // ✅ ADD
    private final ShopConfig      shopConfig;      // ✅ ADD

    // ─────────────────────────────────────────────────────
    // CREDIT OVERVIEW PAGE
    // ─────────────────────────────────────────────────────
    @GetMapping
    public String creditOverview(Model model) {
        List<Customer> customersWithDues =
                customerService.getCustomersWithPendingBalance();

        model.addAttribute("customers",    customersWithDues);
        model.addAttribute("totalPending",
                customerService.getTotalPendingBalance());
        model.addAttribute("activePage",   "credit");
        model.addAttribute("pageTitle",    "Credit / Udhari");
        return "credit/overview";
    }
    @GetMapping("/qr")
    @ResponseBody
    public String getQr(
            @RequestParam BigDecimal amount,
            @RequestParam Long customerId) {

        Customer customer = customerService.getCustomerById(customerId);

        return qrCodeService.generateUpiQrForView(
                amount,
                "UDHARI-" + customer.getName()
        );
    }
    // ─────────────────────────────────────────────────────
    // SHOW PAYMENT FORM
    // ✅ Pass QR code to view
    // ─────────────────────────────────────────────────────
    @GetMapping("/pay/{customerId}")
    public String showPaymentForm(
            @PathVariable Long customerId,
            @RequestParam(required = false) BigDecimal amount,
            Model model) {

        Customer customer = customerService.getCustomerById(customerId);

        BigDecimal balance = customer.getBalance() != null
                ? customer.getBalance() : BigDecimal.ZERO;

        BigDecimal selectedAmount = (amount != null && amount.compareTo(BigDecimal.ZERO) > 0)
                ? amount
                : balance;

        double balanceAmt = balance.doubleValue();
        double halfAmt    = balanceAmt / 2;

        String upiQrBase64 = null;
        if (qrCodeService.isUpiConfigured()
                && selectedAmount.compareTo(BigDecimal.ZERO) > 0) {

            upiQrBase64 = qrCodeService.generateUpiQrForView(
                    selectedAmount,
                    "UDHARI-" + customer.getName());
        }

        model.addAttribute("customer", customer);
        model.addAttribute("balanceAmt", balanceAmt);
        model.addAttribute("halfAmt", halfAmt);
        model.addAttribute("upiQrBase64", upiQrBase64);
        model.addAttribute("qrAmount", selectedAmount); // ✅ IMPORTANT
        model.addAttribute("upiId",
                qrCodeService.isUpiConfigured()
                        ? shopConfig.getUpiId() : null);

        return "credit/pay";
    }

    // ✅ FIXED: customerId as @RequestParam (simpler, works with fetch)
    @GetMapping("/qr/{customerId}")
    @ResponseBody
    public java.util.Map<String, String> generateQr(
            @PathVariable Long customerId,
            @RequestParam BigDecimal amount) {

        java.util.Map<String, String> result = new java.util.HashMap<>();
        try {
            Customer customer =
                    customerService.getCustomerById(customerId);
            String qr = qrCodeService.generateUpiQrForView(
                    amount, "UDHARI-" + customer.getName());
            if (qr != null) {
                result.put("status", "ok");
                result.put("qr", qr);
                result.put("upiId", shopConfig.getUpiId());
            } else {
                result.put("status", "error");
                result.put("message", "UPI not configured");
            }
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        return result;
    }
    // ─────────────────────────────────────────────────────
    // PROCESS PAYMENT
    // ─────────────────────────────────────────────────────
    @PostMapping("/pay/{customerId}")
    public String processPayment(
            @PathVariable Long customerId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description,
            @RequestParam(required = false,
                    defaultValue = "CASH") String paymentMode,
            @RequestParam(required = false) BigDecimal cashPart,
            @RequestParam(required = false) BigDecimal upiPart,
            RedirectAttributes redirectAttributes) {

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please enter a valid payment amount.");
            return "redirect:/credit/pay/" + customerId;
        }

        try {
            String desc;
            if ("CASH_UPI".equals(paymentMode)
                    && cashPart != null && upiPart != null) {
                // ✅ Store split details in description
                desc = "Split payment — Cash: ₹" + cashPart
                        + " | UPI: ₹" + upiPart;
            } else if (description != null
                    && !description.isBlank()) {
                desc = description;
            } else {
                desc = paymentMode + " payment received";
            }

            customerService.recordPayment(
                    customerId, amount, desc);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment of ₹" + amount
                            + " recorded via " + paymentMode + "!");

            return "redirect:/customers/" + customerId;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    e.getMessage());
            return "redirect:/credit/pay/" + customerId;
        }
    }
    // ─────────────────────────────────────────────────────
    // CLEAR ALL DUES
    // ─────────────────────────────────────────────────────
    @PostMapping("/clear/{customerId}")
    public String clearDues(
            @PathVariable Long customerId,
            RedirectAttributes redirectAttributes) {

        try {
            Customer customer =
                    customerService.getCustomerById(customerId);

            BigDecimal credit = customer.getTotalCredit() != null
                    ? customer.getTotalCredit() : BigDecimal.ZERO;
            BigDecimal paid   = customer.getTotalPaid()   != null
                    ? customer.getTotalPaid()   : BigDecimal.ZERO;
            BigDecimal pendingBalance = credit.subtract(paid);

            log.info("clearDues — customer: {} | balance: {}",
                    customer.getName(), pendingBalance);

            if (pendingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        customer.getName()
                                + " has no pending dues to clear.");
                return "redirect:/credit";
            }

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
                    "Error: " + e.getMessage());
        }

        return "redirect:/credit";
    }
}