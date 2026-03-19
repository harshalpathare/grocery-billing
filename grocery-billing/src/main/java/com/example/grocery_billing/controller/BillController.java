package com.example.grocery_billing.controller;


import java.math.BigDecimal;
import java.util.Map;
import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.entity.Customer;
import com.example.grocery_billing.service.BillService;
import com.example.grocery_billing.service.CustomerService;
import com.example.grocery_billing.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * BILL CONTROLLER
 *
 * URL map:
 *   GET  /bills              → list all bills
 *   GET  /bills/new          → billing page (create bill)
 *   POST /bills/new          → save bill
 *   GET  /bills/{id}         → view single bill
 *   POST /bills/{id}/delete  → delete bill
 */
@Controller
@RequestMapping("/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService     billService;
    private final CustomerService customerService;
    private final ProductService  productService;
    // ✅ ADD THIS
    private final com.example.grocery_billing.repository
            .TransactionRepository transactionRepository;
    // ─────────────────────────────────────────────────────
    // LIST ALL BILLS
    // ─────────────────────────────────────────────────────
    @GetMapping
    public String listBills(Model model) {
        List<Bill> bills = billService.getAllBills();

        model.addAttribute("bills",        bills);
        model.addAttribute("activePage",   "bills");
        model.addAttribute("pageTitle",    "All Bills");
        model.addAttribute("todaySales",   billService.getTodaySales());
        model.addAttribute("todayCount",   billService.getTodayBillCount());

        return "bill/list";
    }

    // ─────────────────────────────────────────────────────
    // SHOW CREATE BILL PAGE
    // ─────────────────────────────────────────────────────
    @GetMapping("/new")
    public String showCreateBill(Model model) {
        model.addAttribute("customers",  customerService.getAllActiveCustomers());
        model.addAttribute("activePage", "billing");
        model.addAttribute("pageTitle",  "New Bill");
        model.addAttribute("nextBillNo", billService.generateBillNumber());
        return "bill/create";
    }
    // ── RECEIPT PRINT PAGE (for thermal/receipt printer) ──
    @GetMapping("/{id}/receipt")
    public String receiptView(@PathVariable Long id, Model model) {
        Bill bill = billService.getBillById(id);

        // Amount in words
        BigDecimal total = bill.getTotalAmount() != null
                ? bill.getTotalAmount() : BigDecimal.ZERO;
        long rupees = total.longValue();
        int paise   = total.remainder(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .intValue();

        model.addAttribute("bill",         bill);
        model.addAttribute("rupees",       rupees);
        model.addAttribute("paise",        paise);
        model.addAttribute("pageTitle",    "Receipt " + bill.getBillNo());
        return "bill/receipt";
    }
    // ─────────────────────────────────────────────────────
    // SAVE BILL
    // The form sends items as indexed arrays:
    //   productIds[0], quantities[0], unitPrices[0]
    //   productIds[1], quantities[1], unitPrices[1]  ...etc
    // ─────────────────────────────────────────────────────

    @PostMapping("/new")
    public String saveBill(
            @RequestParam(required = false) Long       customerId,
            @RequestParam(required = false) String     customBillNo,
            @RequestParam(required = false) String     billDate,
            @RequestParam(defaultValue = "false")      boolean    isGst,
            @RequestParam(defaultValue = "PAID")       String     paymentStatus,
            @RequestParam(defaultValue = "CASH")       String     paymentMethod,
            @RequestParam(defaultValue = "0")          BigDecimal transportCost,
            @RequestParam(defaultValue = "0")          BigDecimal extraCost,
            @RequestParam(defaultValue = "0")          BigDecimal discount,
            @RequestParam(required = false)            String     notes,
            @RequestParam(defaultValue = "en")         String     invoiceLang,
            @RequestParam(required = false)            BigDecimal paidAmount,   // ← NEW
            @RequestParam(required = false)            BigDecimal creditAmount, // ← NEW
            @RequestParam("productIds")                List<Long>       productIds,
            @RequestParam("quantities")                List<BigDecimal> quantities,
            @RequestParam("unitPrices")                List<BigDecimal> unitPrices,
            RedirectAttributes redirectAttributes) {

        if (productIds == null || productIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please add at least one product to the bill.");
            return "redirect:/bills/new";
        }

        try {
            Bill bill = new Bill();
            bill.setIsGst(isGst);
            bill.setPaymentStatus(Bill.PaymentStatus.valueOf(paymentStatus));
            bill.setPaymentMethod(paymentMethod);
            bill.setTransportCost(transportCost);
            bill.setExtraCost(extraCost);
            bill.setDiscount(discount);
            bill.setNotes(notes);

            // ✅ Store paid amount for partial bills
            // We use the 'discount' field repurposed, OR add a new field
            // Best approach: store paidAmount in notes for now
            if ("PARTIAL".equals(paymentStatus) && paidAmount != null) {
                String partialNote = "Partial Payment: Paid ₹" + paidAmount
                        + " | On Credit: ₹" + (creditAmount != null ? creditAmount : "0");
                bill.setNotes(notes != null && !notes.isBlank()
                        ? notes + " | " + partialNote
                        : partialNote);
            }

            // Handle custom bill date
            if (billDate != null && !billDate.trim().isEmpty()) {
                bill.setBillDate(java.time.LocalDate.parse(billDate));
            }

            // Handle custom bill number
            if (customBillNo != null && !customBillNo.trim().isEmpty()) {
                bill.setBillNo(customBillNo.trim());
            }

            // Attach customer
            if (customerId != null) {
                Customer customer = customerService.getCustomerById(customerId);
                bill.setCustomer(customer);
            }

            // Build items
            List<BillService.BillItemRequest> items = new ArrayList<>();
            for (int i = 0; i < productIds.size(); i++) {
                if (productIds.get(i) != null
                        && quantities.get(i) != null
                        && quantities.get(i).compareTo(BigDecimal.ZERO) > 0) {
                    items.add(new BillService.BillItemRequest(
                            productIds.get(i),
                            quantities.get(i),
                            unitPrices.get(i),
                            invoiceLang
                    ));
                }
            }

            if (items.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "No valid items found. Please add products.");
                return "redirect:/bills/new";
            }

            // ✅ Pass paidAmount and creditAmount to service
            Bill savedBill = billService.createBill(
                    bill, items, paidAmount, creditAmount);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Bill " + savedBill.getBillNo() + " created successfully!");
            return "redirect:/bills/" + savedBill.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error creating bill: " + e.getMessage());
            return "redirect:/bills/new";
        }
    }
    // ─────────────────────────────────────────────────────
    // VIEW SINGLE BILL
    // ─────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public String viewBill(@PathVariable Long id, Model model) {
        Bill bill = billService.getBillById(id);

        // Pre-calculate GST per item
        Map<Long, BigDecimal> itemGstAmounts = new java.util.HashMap<>();
        if (Boolean.TRUE.equals(bill.getIsGst())) {
            for (com.example.grocery_billing.entity.BillItem item
                    : bill.getBillItems()) {
                if (item.getGstPercent() != null
                        && item.getItemTotal() != null) {
                    BigDecimal gstAmt = item.getItemTotal()
                            .multiply(item.getGstPercent())
                            .divide(BigDecimal.valueOf(100), 2,
                                    java.math.RoundingMode.HALF_UP);
                    itemGstAmounts.put(item.getId(), gstAmt);
                }
            }
        }

        // ✅ NEW: Find credit/paid amounts from transaction for this bill
        BigDecimal partialCreditAmount = BigDecimal.ZERO;
        BigDecimal partialPaidAmount   = BigDecimal.ZERO;

        if (Bill.PaymentStatus.PARTIAL.equals(bill.getPaymentStatus())
                || Bill.PaymentStatus.CREDIT.equals(bill.getPaymentStatus())) {

            // Find the credit transaction for this bill
            List<com.example.grocery_billing.entity.Transaction> billTxns =
                    transactionRepository.findByBillId(id);

            // Credit amount = sum of CREDIT transactions for this bill
            partialCreditAmount = billTxns.stream()
                    .filter(t -> com.example.grocery_billing.entity
                            .Transaction.TransactionType.CREDIT
                            .equals(t.getType()))
                    .map(com.example.grocery_billing.entity.Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Paid amount = total - credit
            partialPaidAmount = bill.getTotalAmount()
                    .subtract(partialCreditAmount);
            if (partialPaidAmount.compareTo(BigDecimal.ZERO) < 0) {
                partialPaidAmount = BigDecimal.ZERO;
            }
        }

        model.addAttribute("bill",                bill);
        model.addAttribute("itemGstAmounts",      itemGstAmounts);
        model.addAttribute("partialCreditAmount", partialCreditAmount);
        model.addAttribute("partialPaidAmount",   partialPaidAmount);
        model.addAttribute("activePage",          "bills");
        model.addAttribute("pageTitle",
                "Bill " + bill.getBillNo());

        return "bill/view";
    }

    // ─────────────────────────────────────────────────────
    // DELETE BILL
    // ─────────────────────────────────────────────────────
    @PostMapping("/{id}/delete")
    public String deleteBill(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            billService.deleteBill(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Bill deleted successfully.");
        } catch (Exception e) {
            // Show the actual error so you can debug
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error deleting bill: " + e.getMessage());
            e.printStackTrace(); // prints full stack trace to console
        }
        return "redirect:/bills";
    }
}