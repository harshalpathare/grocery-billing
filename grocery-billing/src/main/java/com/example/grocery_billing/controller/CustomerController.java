package com.example.grocery_billing.controller;


import com.example.grocery_billing.entity.Customer;
import com.example.grocery_billing.entity.Transaction;
import com.example.grocery_billing.service.CustomerService;
import com.example.grocery_billing.service.ShopFeatureService;
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

    private final CustomerService    customerService;
    private final ShopFeatureService shopFeatureService;

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
    public String viewCustomer(@PathVariable Long id, Model model) {
        Customer customer = customerService.getCustomerById(id);
        List<Transaction> transactions =
                customerService.getTransactionHistory(id);
        List<com.example.grocery_billing.entity.Bill> bills =
                customerService.getPurchaseHistory(id);

        model.addAttribute("customer", customer);
        model.addAttribute("transactions", transactions);
        model.addAttribute("bills", bills);
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
    public String showEditForm(@PathVariable Long id, Model model) {
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
            @PathVariable Long id,
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
            @PathVariable Long id,
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

    // ─────────────────────────────────────────────────────
    // CUSTOMER CSV EXPORT — Full Details
    // ─────────────────────────────────────────────────────
    @GetMapping("/export/csv")
    public org.springframework.http.ResponseEntity<byte[]> exportCsv() {
        java.util.List<com.example.grocery_billing.entity.Customer> customers = customerService.getAllActiveCustomers();
        boolean loyaltyEnabled = Boolean.TRUE.equals(
                shopFeatureService.getFeaturesForCurrentShop().get("enable_loyalty"));

        StringBuilder sw = new StringBuilder();

        // Header — loyalty column only if feature is ON
        sw.append("ID,Name,Phone,Email,Address,GSTIN,Credit Limit,");
        sw.append("Total Credit Taken,Total Paid,Pending Balance,");
        if (loyaltyEnabled) sw.append("Loyalty Points,");
        sw.append("Status,Member Since\n");

        for (com.example.grocery_billing.entity.Customer c : customers) {
            sw.append(c.getId()).append(",");
            sw.append(escape(c.getName())).append(",");
            sw.append(escape(c.getPhone())).append(",");
            sw.append(escape(c.getEmail())).append(",");
            sw.append(escape(c.getAddress())).append(",");
            sw.append(escape(c.getGstin())).append(",");
            sw.append(fmt(c.getCreditLimit())).append(",");
            sw.append(fmt(c.getTotalCredit())).append(",");
            sw.append(fmt(c.getTotalPaid())).append(",");
            sw.append(fmt(c.getBalance())).append(",");
            if (loyaltyEnabled)
                sw.append(c.getLoyaltyPoints() != null ? c.getLoyaltyPoints() : 0).append(",");
            sw.append(Boolean.TRUE.equals(c.getActive()) ? "Active" : "Inactive").append(",");
            sw.append(c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate().toString() : "").append("\n");
        }

        byte[] bytes = sw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String filename = "Customers_" + java.time.LocalDate.now() + ".csv";
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    // ─────────────────────────────────────────────────────
    // CUSTOMER EXCEL EXPORT — Full Details
    // ─────────────────────────────────────────────────────
    @GetMapping("/export/excel")
    public org.springframework.http.ResponseEntity<byte[]> exportExcel() {
        try {
            java.util.List<com.example.grocery_billing.entity.Customer> customers = customerService.getAllActiveCustomers();
            boolean loyaltyEnabled = Boolean.TRUE.equals(
                    shopFeatureService.getFeaturesForCurrentShop().get("enable_loyalty"));

            org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Customers");

            // Header style — dark blue
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            // Title row
            int totalCols = loyaltyEnabled ? 13 : 12;
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Customer Details Report — Generated: " + java.time.LocalDate.now());
            org.apache.poi.ss.usermodel.CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, totalCols - 1));

            // Summary row
            org.apache.poi.ss.usermodel.Row summaryRow = sheet.createRow(1);
            summaryRow.createCell(0).setCellValue("Total Customers:");
            summaryRow.createCell(1).setCellValue(customers.size());

            // Blank row
            sheet.createRow(2);

            // Build header list conditionally
            java.util.List<String> headerList = new java.util.ArrayList<>(java.util.Arrays.asList(
                "#", "Name", "Phone", "Email", "Address", "GSTIN",
                "Credit Limit (₹)", "Total Credit (₹)", "Total Paid (₹)", "Pending Balance (₹)"
            ));
            if (loyaltyEnabled) headerList.add("Loyalty Points ⭐");
            headerList.add("Status");
            headerList.add("Member Since");
            String[] headers = headerList.toArray(new String[0]);

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Amount style
            org.apache.poi.ss.usermodel.CellStyle amtStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.DataFormat df = workbook.createDataFormat();
            amtStyle.setDataFormat(df.getFormat("#,##0.00"));

            // Red style for pending balance
            org.apache.poi.ss.usermodel.CellStyle redStyle = workbook.createCellStyle();
            redStyle.setDataFormat(df.getFormat("#,##0.00"));
            org.apache.poi.ss.usermodel.Font redFont = workbook.createFont();
            redFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.RED.getIndex());
            redFont.setBold(true);
            redStyle.setFont(redFont);

            // Green style for status
            org.apache.poi.ss.usermodel.CellStyle greenStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font greenFont = workbook.createFont();
            greenFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.GREEN.getIndex());
            greenFont.setBold(true);
            greenStyle.setFont(greenFont);

            // Data rows
            int rowNum = 4;
            int idx = 1;
            for (com.example.grocery_billing.entity.Customer c : customers) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(idx++);
                row.createCell(1).setCellValue(c.getName() != null ? c.getName() : "");
                row.createCell(2).setCellValue(c.getPhone() != null ? c.getPhone() : "");
                row.createCell(3).setCellValue(c.getEmail() != null ? c.getEmail() : "");
                row.createCell(4).setCellValue(c.getAddress() != null ? c.getAddress() : "");
                row.createCell(5).setCellValue(c.getGstin() != null ? c.getGstin() : "");

                org.apache.poi.ss.usermodel.Cell limitCell = row.createCell(6);
                limitCell.setCellValue(c.getCreditLimit() != null ? c.getCreditLimit().doubleValue() : 0);
                limitCell.setCellStyle(amtStyle);

                org.apache.poi.ss.usermodel.Cell creditCell = row.createCell(7);
                creditCell.setCellValue(c.getTotalCredit() != null ? c.getTotalCredit().doubleValue() : 0);
                creditCell.setCellStyle(amtStyle);

                org.apache.poi.ss.usermodel.Cell paidCell = row.createCell(8);
                paidCell.setCellValue(c.getTotalPaid() != null ? c.getTotalPaid().doubleValue() : 0);
                paidCell.setCellStyle(amtStyle);

                org.apache.poi.ss.usermodel.Cell balCell = row.createCell(9);
                double bal = c.getBalance() != null ? c.getBalance().doubleValue() : 0;
                balCell.setCellValue(bal);
                balCell.setCellStyle(bal > 0 ? redStyle : amtStyle);

                int nextCol = 10;
                if (loyaltyEnabled) {
                    row.createCell(nextCol++).setCellValue(
                        c.getLoyaltyPoints() != null ? c.getLoyaltyPoints() : 0);
                }

                org.apache.poi.ss.usermodel.Cell statusCell = row.createCell(nextCol++);
                statusCell.setCellValue(Boolean.TRUE.equals(c.getActive()) ? "Active" : "Inactive");
                statusCell.setCellStyle(Boolean.TRUE.equals(c.getActive()) ? greenStyle : null);

                row.createCell(nextCol).setCellValue(
                    c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate().toString() : ""
                );
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            workbook.write(bos);
            workbook.close();

            String filename = "Customers_" + java.time.LocalDate.now() + ".xlsx";
            return org.springframework.http.ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(org.springframework.http.MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bos.toByteArray());

        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.internalServerError().build();
        }
    }

    private String escape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private String fmt(java.math.BigDecimal val) {
        return val != null ? String.format("%.2f", val) : "0.00";
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam("ids") java.util.List<Long> ids, org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            for (Long id : ids) {
                customerService.deleteCustomer(id);
            }
            ra.addFlashAttribute("successMessage", "Selected customers deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error deleting customers: " + e.getMessage());
        }
        return "redirect:/customers";
    }

}
