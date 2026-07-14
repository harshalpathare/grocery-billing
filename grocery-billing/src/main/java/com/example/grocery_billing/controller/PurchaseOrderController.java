package com.example.grocery_billing.controller;

import com.example.grocery_billing.entity.PurchaseOrder;
import com.example.grocery_billing.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/purchases")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService poService;
    private final SupplierService      supplierService;
    private final ProductService       productService;
    private final ExcelPurchaseLedgerService excelPurchaseLedgerService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("orders",
                poService.getAll());
        model.addAttribute("activePage", "purchases");
        model.addAttribute("pageTitle",  "Purchase Orders");
        return "purchase/list";
    }

    @GetMapping("/export")
    public void exportPurchaseLedger(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"purchase_ledger.xlsx\"");
        excelPurchaseLedgerService.exportPurchaseLedger(response.getOutputStream());
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("suppliers",
                supplierService.getAllActive());
        model.addAttribute("products",
                productService.getAllActiveProducts());
        model.addAttribute("nextPoNo",
                poService.generatePoNumber());
        model.addAttribute("activePage", "purchases");
        model.addAttribute("pageTitle",  "New Purchase Order");
        return "purchase/form";
    }

    @PostMapping("/new")
    public String save(
            @RequestParam(required = false) Long    supplierId,
            @RequestParam(required = false) String  poNumber,
            @RequestParam(required = false) String  orderDate,
            @RequestParam(required = false) String  supplierInvoiceNo,
            @RequestParam(defaultValue = "0")
            BigDecimal taxAmount,
            @RequestParam(required = false) String  notes,
            @RequestParam(defaultValue = "PURCHASE") String type,
            @RequestParam(defaultValue = "true") Boolean isGst,
            @RequestParam("productIds")
            List<Long>       productIds,
            @RequestParam("quantities")
            List<BigDecimal> quantities,
            @RequestParam("unitCosts")
            List<BigDecimal> unitCosts,
            RedirectAttributes ra) {

        try {
            PurchaseOrder po = new PurchaseOrder();

            if (supplierId != null) {
                po.setSupplier(
                        supplierService.getById(supplierId));
            }
            if (poNumber != null && !poNumber.isBlank()) {
                po.setPoNumber(poNumber.trim());
            }
            if (orderDate != null && !orderDate.isBlank()) {
                po.setOrderDate(
                        java.time.LocalDate.parse(orderDate));
            }
            po.setSupplierInvoiceNo(supplierInvoiceNo);
            
            po.setType(PurchaseOrder.OrderType.valueOf(type));
            po.setIsGst(isGst);
            
            // If it's not a GST purchase, force tax to 0
            po.setTaxAmount(isGst ? taxAmount : BigDecimal.ZERO);
            
            po.setNotes(notes);

            // Build items
            List<PurchaseOrderService.PoItemRequest> items =
                    new ArrayList<>();
            // In the items loop — add null check on unitCost
            for (int i = 0; i < productIds.size(); i++) {
                if (productIds.get(i) != null
                        && quantities.get(i) != null
                        && quantities.get(i).compareTo(
                        BigDecimal.ZERO) > 0) {

                    // ✅ Default to 0 if unitCost is null
                    BigDecimal cost = (unitCosts != null
                            && i < unitCosts.size()
                            && unitCosts.get(i) != null)
                            ? unitCosts.get(i)
                            : BigDecimal.ZERO;

                    items.add(
                            new PurchaseOrderService.PoItemRequest(
                                    productIds.get(i),
                                    quantities.get(i),
                                    cost));   // ← use safe cost
                }
            }

            if (items.isEmpty()) {
                ra.addFlashAttribute("errorMessage",
                        "Add at least one product.");
                return "redirect:/purchases/new";
            }

            PurchaseOrder saved =
                    poService.createPurchaseOrder(po, items);
            ra.addFlashAttribute("successMessage",
                    "Purchase Order " + saved.getPoNumber()
                            + " created! Stock updated.");
            return "redirect:/purchases/" + saved.getId();

        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage",
                    "Error: " + e.getMessage());
            return "redirect:/purchases/new";
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        PurchaseOrder po = poService.getById(id);
        model.addAttribute("po",        po);
        model.addAttribute("activePage","purchases");
        model.addAttribute("pageTitle",
                "PO " + po.getPoNumber());
        return "purchase/view";
    }
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         RedirectAttributes ra) {
        try {
            poService.delete(id);
            ra.addFlashAttribute("successMessage",
                    "Purchase Order deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage",
                    "Error: " + e.getMessage());
        }
        return "redirect:/purchases";
    }
    @PostMapping("/{id}/pay")
    public String markPaid(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal amountPaid,
            RedirectAttributes ra) {
        try {
            poService.markAsPaid(id, amountPaid);
            ra.addFlashAttribute("successMessage",
                    "Payment recorded! Status updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage",
                    "Error: " + e.getMessage());
        }
        return "redirect:/purchases/" + id;
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam("ids") java.util.List<Long> ids, org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            for (Long id : ids) {
                poService.delete(id);
            }
            ra.addFlashAttribute("successMessage", "Selected purchases deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error deleting purchases: " + e.getMessage());
        }
        return "redirect:/purchases";
    }

}
