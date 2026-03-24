package com.example.grocery_billing.controller;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.grocery_billing.config.ShopConfig;
import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.entity.Customer;
import com.example.grocery_billing.repository.TransactionRepository;
import com.example.grocery_billing.service.BillService;
import com.example.grocery_billing.service.CustomerService;
import com.example.grocery_billing.service.ProductService;
import com.example.grocery_billing.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/bills")
@RequiredArgsConstructor
@Slf4j
public class BillController {

    private final ShopConfig            shopConfig;
    private final BillService           billService;
    private final CustomerService       customerService;
    private final ProductService        productService;
    private final QrCodeService         qrCodeService;
    private final TransactionRepository transactionRepository;

    // ─────────────────────────────────────────────────────
    // LIST ALL BILLS
    // ─────────────────────────────────────────────────────
    @GetMapping
    public String listBills(Model model) {
        List<Bill> bills = billService.getAllBills();
        model.addAttribute("bills",      bills);
        model.addAttribute("activePage", "bills");
        model.addAttribute("pageTitle",  "All Bills");
        model.addAttribute("todaySales", billService.getTodaySales());
        model.addAttribute("todayCount", billService.getTodayBillCount());
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

    // ─────────────────────────────────────────────────────
    // RECEIPT PRINT PAGE
    // ─────────────────────────────────────────────────────
    @GetMapping("/{id}/receipt")
    public String receiptView(@PathVariable Long id, Model model) {
        Bill bill = billService.getBillById(id);

        BigDecimal total = bill.getTotalAmount() != null
                ? bill.getTotalAmount() : BigDecimal.ZERO;
        long rupees = total.longValue();
        int  paise  = total.remainder(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .intValue();

        String upiQrBase64 = null;
        if (qrCodeService.isUpiConfigured()
                && total.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal qrAmount = null;
            if ("UPI".equals(bill.getPaymentMethod())) {
                qrAmount = total;
            } else if ("CASH_UPI".equals(bill.getPaymentMethod())) {
                qrAmount = extractUpiAmountFromNotes(bill.getNotes());
            }
            if (qrAmount != null
                    && qrAmount.compareTo(BigDecimal.ZERO) > 0) {
                upiQrBase64 = qrCodeService.generateUpiQrForReceipt(
                        qrAmount, bill.getBillNo());
            }
        }

        model.addAttribute("bill",        bill);
        model.addAttribute("rupees",      rupees);
        model.addAttribute("paise",       paise);
        model.addAttribute("upiQrBase64", upiQrBase64);
        model.addAttribute("upiId",
                qrCodeService.isUpiConfigured()
                        ? shopConfig.getUpiId() : null);
        model.addAttribute("pageTitle", "Receipt " + bill.getBillNo());
        return "bill/receipt";
    }

    // ─────────────────────────────────────────────────────
    // SAVE BILL
    // ─────────────────────────────────────────────────────
    @PostMapping("/new")
    public String saveBill(
            @RequestParam(required = false)        Long       customerId,
            @RequestParam(required = false)        String     customBillNo,
            @RequestParam(required = false)        String     billDate,
            @RequestParam(defaultValue = "false")  boolean    isGst,
            @RequestParam(defaultValue = "CASH")   String     paymentMethod,
            @RequestParam(required = false)        BigDecimal cashPaidAmount,
            @RequestParam(required = false)        BigDecimal upiAmount,
            @RequestParam(defaultValue = "0")      BigDecimal transportCost,
            @RequestParam(defaultValue = "0")      BigDecimal extraCost,
            @RequestParam(defaultValue = "0")      BigDecimal discount,
            @RequestParam(required = false)        String     notes,
            @RequestParam(defaultValue = "en")     String     invoiceLang,
            @RequestParam(required = false)        BigDecimal paidAmount,
            @RequestParam(required = false)        BigDecimal creditAmount,
            @RequestParam("productIds")            List<Long>       productIds,
            @RequestParam("quantities")            List<BigDecimal> quantities,
            @RequestParam("unitPrices")            List<BigDecimal> unitPrices,
            RedirectAttributes redirectAttributes) {

        if (productIds == null || productIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Please add at least one product.");
            return "redirect:/bills/new";
        }

        try {
            Bill bill = new Bill();
            bill.setIsGst(isGst);
            bill.setPaymentMethod(paymentMethod);
            bill.setTransportCost(transportCost);
            bill.setExtraCost(extraCost);
            bill.setDiscount(discount);

            // ── STEP 1: Resolve payment status FROM method ──
            // This is the SINGLE source of truth — done ONCE
            // paymentStatus param from form is IGNORED entirely
            Bill.PaymentStatus resolvedStatus;

            if ("CREDIT".equals(paymentMethod)) {
                resolvedStatus = Bill.PaymentStatus.CREDIT;
            } else if ("CREDIT_UPI".equals(paymentMethod)) {
                resolvedStatus = Bill.PaymentStatus.PARTIAL;
            } else {
                // CASH, UPI, CASH_UPI, CARD, NEFT → PAID
                resolvedStatus = Bill.PaymentStatus.PAID;
            }

            bill.setPaymentStatus(resolvedStatus);

            log.info("saveBill — method: {} → status: {}",
                    paymentMethod, resolvedStatus);

            // ── STEP 2: Build notes based on method ──────────
            String baseNotes = notes != null
                    && !notes.isBlank() ? notes : null;

            if ("CASH_UPI".equals(paymentMethod)) {
                BigDecimal cash = cashPaidAmount != null
                        ? cashPaidAmount : BigDecimal.ZERO;
                BigDecimal upi  = upiAmount != null
                        ? upiAmount : BigDecimal.ZERO;
                String splitNote = "Cash: ₹" + cash
                        + " | UPI: ₹" + upi;
                bill.setNotes(baseNotes != null
                        ? baseNotes + " | " + splitNote
                        : splitNote);

            } else if ("CREDIT_UPI".equals(paymentMethod)) {
                BigDecimal paid   = paidAmount != null
                        ? paidAmount : BigDecimal.ZERO;
                BigDecimal credit = creditAmount != null
                        ? creditAmount : BigDecimal.ZERO;
                String splitNote = "UPI: ₹" + paid
                        + " | Credit (Udhari): ₹" + credit;
                bill.setNotes(baseNotes != null
                        ? baseNotes + " | " + splitNote
                        : splitNote);

            } else {
                bill.setNotes(baseNotes);
            }

            // ── STEP 3: Custom date / bill number ────────────
            if (billDate != null && !billDate.trim().isEmpty()) {
                bill.setBillDate(
                        java.time.LocalDate.parse(billDate));
            }
            if (customBillNo != null
                    && !customBillNo.trim().isEmpty()) {
                bill.setBillNo(customBillNo.trim());
            }

            // ── STEP 4: Attach customer ───────────────────────
            if (customerId != null) {
                Customer customer =
                        customerService.getCustomerById(customerId);
                bill.setCustomer(customer);
            }

            // ── STEP 5: Build items ───────────────────────────
            List<BillService.BillItemRequest> items =
                    new ArrayList<>();
            for (int i = 0; i < productIds.size(); i++) {
                if (productIds.get(i) != null
                        && quantities.get(i) != null
                        && quantities.get(i).compareTo(
                        BigDecimal.ZERO) > 0) {
                    items.add(new BillService.BillItemRequest(
                            productIds.get(i),
                            quantities.get(i),
                            unitPrices.get(i),
                            invoiceLang));
                }
            }

            if (items.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "No valid items found.");
                return "redirect:/bills/new";
            }

            // ── STEP 6: Save ──────────────────────────────────
            // For CREDIT_UPI: paidAmount = UPI portion paid now
            //                 creditAmount = goes to udhari
            // For CREDIT:     paidAmount = null, creditAmount = null
            //                 BillService uses full total as credit
            Bill savedBill = billService.createBill(
                    bill, items, paidAmount, creditAmount);

            log.info("Bill saved: {} | status: {} | method: {}",
                    savedBill.getBillNo(),
                    savedBill.getPaymentStatus(),
                    savedBill.getPaymentMethod());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Bill " + savedBill.getBillNo()
                            + " created successfully!");
            return "redirect:/bills/" + savedBill.getId();

        } catch (Exception e) {
            log.error("Error creating bill: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error: " + e.getMessage());
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
        Map<Long, BigDecimal> itemGstAmounts =
                new java.util.HashMap<>();
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

        // Credit/partial amounts from transactions
        BigDecimal partialCreditAmount = BigDecimal.ZERO;
        BigDecimal partialPaidAmount   = BigDecimal.ZERO;

        if (Bill.PaymentStatus.PARTIAL.equals(
                bill.getPaymentStatus())
                || Bill.PaymentStatus.CREDIT.equals(
                bill.getPaymentStatus())) {

            List<com.example.grocery_billing.entity.Transaction>
                    billTxns = transactionRepository
                    .findByBillId(id);

            partialCreditAmount = billTxns.stream()
                    .filter(t -> com.example.grocery_billing
                            .entity.Transaction.TransactionType
                            .CREDIT.equals(t.getType()))
                    .map(com.example.grocery_billing.entity
                            .Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            partialPaidAmount = bill.getTotalAmount()
                    .subtract(partialCreditAmount);
            if (partialPaidAmount.compareTo(BigDecimal.ZERO) < 0) {
                partialPaidAmount = BigDecimal.ZERO;
            }
        }

        // Generate UPI QR
        String upiQrBase64 = null;
        if (qrCodeService.isUpiConfigured()
                && bill.getTotalAmount() != null
                && bill.getTotalAmount().compareTo(
                BigDecimal.ZERO) > 0) {

            BigDecimal qrAmount = null;
            if ("UPI".equals(bill.getPaymentMethod())) {
                qrAmount = bill.getTotalAmount();
            } else if ("CASH_UPI".equals(
                    bill.getPaymentMethod())) {
                qrAmount = extractUpiAmountFromNotes(
                        bill.getNotes());
            }

            if (qrAmount != null
                    && qrAmount.compareTo(BigDecimal.ZERO) > 0) {
                upiQrBase64 = qrCodeService.generateUpiQrForView(
                        qrAmount, bill.getBillNo());
            }
        }

        model.addAttribute("bill",                bill);
        model.addAttribute("itemGstAmounts",      itemGstAmounts);
        model.addAttribute("partialCreditAmount", partialCreditAmount);
        model.addAttribute("partialPaidAmount",   partialPaidAmount);
        model.addAttribute("upiQrBase64",         upiQrBase64);
        model.addAttribute("upiId",
                qrCodeService.isUpiConfigured()
                        ? shopConfig.getUpiId() : null);
        model.addAttribute("activePage", "bills");
        model.addAttribute("pageTitle",  "Bill " + bill.getBillNo());
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
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error deleting bill: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/bills";
    }

    // ─────────────────────────────────────────────────────
    // QR CODE API — for billing page Cash+UPI split
    // ─────────────────────────────────────────────────────
    @GetMapping("/qr")
    @ResponseBody
    public java.util.Map<String, String> generateQr(
            @RequestParam BigDecimal amount,
            @RequestParam(required = false,
                    defaultValue = "Bill") String note) {

        java.util.Map<String, String> result =
                new java.util.HashMap<>();
        try {
            String qr = qrCodeService.generateUpiQrForView(
                    amount, note);
            if (qr != null) {
                result.put("status", "ok");
                result.put("qr",     qr);
                result.put("upiId",  shopConfig.getUpiId());
            } else {
                result.put("status",  "error");
                result.put("message", "UPI not configured");
            }
        } catch (Exception e) {
            result.put("status",  "error");
            result.put("message", e.getMessage());
        }
        return result;
    }
    // ─────────────────────────────────────────────────────
// SEND BILL ON WHATSAPP
// ─────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────
    // HELPER — Extract UPI amount from notes
    // ─────────────────────────────────────────────────────
    private BigDecimal extractUpiAmountFromNotes(String notes) {
        if (notes == null || notes.isBlank()) return null;
        try {
            int idx = notes.indexOf("UPI: ₹");
            if (idx == -1) idx = notes.indexOf("UPI: Rs.");
            if (idx == -1) return null;
            String after = notes.substring(idx + 6).trim();
            after = after.replace("₹", "")
                    .replace("Rs.", "").trim();
            String[] parts = after.split("[\\s|]");
            return new BigDecimal(parts[0].trim());
        } catch (Exception e) {
            return null;
        }
    }
}