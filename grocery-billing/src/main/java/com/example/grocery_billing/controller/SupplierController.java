package com.example.grocery_billing.controller;

import com.example.grocery_billing.entity.PurchaseOrder;
import com.example.grocery_billing.entity.Supplier;
import com.example.grocery_billing.service.PurchaseOrderService;
import com.example.grocery_billing.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;
    // Add to SupplierController fields
    private final PurchaseOrderService poService;
    @GetMapping
    public String list(Model model) {
        model.addAttribute("suppliers",
                supplierService.getAllActive());
        model.addAttribute("totalPayable",
                supplierService.getTotalPayable());
        model.addAttribute("activePage", "suppliers");
        model.addAttribute("pageTitle",  "Suppliers");
        return "supplier/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("supplier", new Supplier());
        model.addAttribute("isEdit",    false);
        model.addAttribute("pageTitle", "Add Supplier");
        return "supplier/form";
    }

    @PostMapping("/new")
    public String save(
            @ModelAttribute Supplier supplier,
            RedirectAttributes ra) {
        try {
            supplierService.save(supplier);
            ra.addFlashAttribute("successMessage",
                    "Supplier added successfully!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage",
                    e.getMessage());
        }
        return "redirect:/suppliers";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           Model model) {
        model.addAttribute("supplier",
                supplierService.getById(id));
        model.addAttribute("isEdit",    true);
        model.addAttribute("pageTitle", "Edit Supplier");
        return "supplier/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute Supplier supplier,
                         RedirectAttributes ra) {
        try {
            supplier.setId(id);
            supplierService.save(supplier);
            ra.addFlashAttribute("successMessage",
                    "Supplier updated!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage",
                    e.getMessage());
        }
        return "redirect:/suppliers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         RedirectAttributes ra) {
        supplierService.delete(id);
        ra.addFlashAttribute("successMessage",
                "Supplier removed.");
        return "redirect:/suppliers";
    }

    // ✅ FIXED: supplier list pay button
// should record against a PO, not directly
    @PostMapping("/{id}/pay")
    public String pay(
            @PathVariable Long id,
            @RequestParam java.math.BigDecimal amount,
            RedirectAttributes ra) {
        try {
            Supplier s = supplierService.getById(id);

            BigDecimal currentPaid =
                    s.getTotalPaid() != null
                            ? s.getTotalPaid() : BigDecimal.ZERO;
            BigDecimal payable =
                    s.getTotalPayable() != null
                            ? s.getTotalPayable() : BigDecimal.ZERO;
            BigDecimal balance =
                    payable.subtract(currentPaid);

            if (balance.compareTo(
                    java.math.BigDecimal.ZERO) <= 0) {
                ra.addFlashAttribute("errorMessage",
                        "No pending balance.");
                return "redirect:/suppliers";
            }

            // Clamp to max balance
            java.math.BigDecimal safeAmt =
                    amount.min(balance);

            // ✅ Update supplier balance
            s.setTotalPaid(currentPaid.add(safeAmt));
            s.updateBalance();
            supplierService.save(s);

            // ✅ Update PO statuses for this supplier
            updatePoStatuses(id, safeAmt);

            ra.addFlashAttribute("successMessage",
                    "Payment of ₹" + safeAmt
                            + " recorded successfully!");

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage",
                    e.getMessage());
        }
        return "redirect:/suppliers";
    }

    // ── Helper: update PO payment statuses ───────────────
    private void updatePoStatuses(Long supplierId,
                                  java.math.BigDecimal paidNow) {

        // Get all PENDING/PARTIAL POs for this supplier
        List<PurchaseOrder>
                pendingPos = poService.getBySupplier(supplierId)
                .stream()
                .filter(po -> po.getPaymentStatus() !=
                        com.example.grocery_billing.entity
                                .PurchaseOrder.PaymentStatus.PAID)
                .collect(java.util.stream.Collectors.toList());

        java.math.BigDecimal remaining = paidNow;

        for (com.example.grocery_billing.entity
                .PurchaseOrder po : pendingPos) {

            if (remaining.compareTo(
                    java.math.BigDecimal.ZERO) <= 0) break;

            java.math.BigDecimal poTotal =
                    po.getTotalAmount() != null
                            ? po.getTotalAmount()
                            : java.math.BigDecimal.ZERO;
            java.math.BigDecimal poAlreadyPaid =
                    po.getAmountPaid() != null
                            ? po.getAmountPaid()
                            : java.math.BigDecimal.ZERO;
            java.math.BigDecimal poDue =
                    poTotal.subtract(poAlreadyPaid);

            if (poDue.compareTo(
                    java.math.BigDecimal.ZERO) <= 0) continue;

            if (remaining.compareTo(poDue) >= 0) {
                // ✅ Fully pay this PO
                po.setAmountPaid(poTotal);
                po.setPaymentStatus(
                        com.example.grocery_billing.entity
                                .PurchaseOrder.PaymentStatus.PAID);
                remaining = remaining.subtract(poDue);
            } else {
                // ✅ Partially pay this PO
                po.setAmountPaid(
                        poAlreadyPaid.add(remaining));
                po.setPaymentStatus(
                        com.example.grocery_billing.entity
                                .PurchaseOrder.PaymentStatus.PARTIAL);
                remaining = java.math.BigDecimal.ZERO;
            }

            poService.saveDirectly(po);
        }
    }
}