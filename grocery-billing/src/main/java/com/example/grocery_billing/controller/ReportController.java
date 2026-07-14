package com.example.grocery_billing.controller;

import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.service.ExcelExportService;
import com.example.grocery_billing.service.ProfitReportService;
import com.example.grocery_billing.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ExcelExportService excelExportService;

    // ─────────────────────────────────────────────────────
    // MAIN REPORTS PAGE
    // ─────────────────────────────────────────────────────
    @GetMapping
    public String reportsHome(Model model) {
        ReportService.SummaryStats stats = reportService.getSummaryStats();

        LocalDate now = LocalDate.now();
        List<ReportService.MonthlyRow> monthlyData =
                reportService.getMonthlyReport(now.getYear());

        ReportService.DailyReport todayReport =
                reportService.getDailyReport(now);

        ReportService.CreditReport creditReport =
                reportService.getCreditReport();
// ✅ Pre-calculate whether any month has sales
        boolean hasAnySales = monthlyData.stream()
                .anyMatch(r -> r.sales().compareTo(BigDecimal.ZERO) > 0);

        model.addAttribute("hasAnySales", hasAnySales);
        // Pre-calculate bar widths for index page chart
        // Uses MAX sales month as 100% — all others scale relative to it
        // ✅ FIX: if maxSales is zero, all bars should be 0 width
        BigDecimal maxSales = monthlyData.stream()
                .map(ReportService.MonthlyRow::sales)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        List<Integer> indexBarWidths = monthlyData.stream()
                .map(row -> maxSales.compareTo(BigDecimal.ZERO) > 0
                        && row.sales().compareTo(BigDecimal.ZERO) > 0
                        ? row.sales()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(maxSales, 0, RoundingMode.HALF_UP)
                        .intValue()
                        : 0)
                .toList();
        model.addAttribute("stats",          stats);
        model.addAttribute("monthlyData",    monthlyData);
        model.addAttribute("indexBarWidths", indexBarWidths);
        model.addAttribute("todayReport",    todayReport);
        model.addAttribute("creditReport",   creditReport);
        model.addAttribute("currentYear",    now.getYear());
        model.addAttribute("activePage",     "reports");
        model.addAttribute("pageTitle",      "Reports & Analytics");

        return "report/index";
    }

    // ─────────────────────────────────────────────────────
    // DAILY REPORT
    // ─────────────────────────────────────────────────────
    @GetMapping("/daily")
    public String dailyReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        if (date == null) date = LocalDate.now();

        ReportService.DailyReport report = reportService.getDailyReport(date);

        // Pre-calculate subtotal sum — never do stream/reduce in Thymeleaf
        BigDecimal dailySubtotal = report.bills().stream()
                .map(b -> b.getSubtotal() != null ? b.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("report",        report);
        model.addAttribute("dailySubtotal", dailySubtotal);
        model.addAttribute("date",          date);
        model.addAttribute("prevDate",      date.minusDays(1));
        model.addAttribute("nextDate",      date.plusDays(1));
        model.addAttribute("isToday",       date.equals(LocalDate.now()));
        model.addAttribute("activePage",    "reports");
        model.addAttribute("pageTitle",     "Daily Report — "
                + date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));

        return "report/daily";
    }

    // ─────────────────────────────────────────────────────
    // MONTHLY REPORT
    // ─────────────────────────────────────────────────────
    @GetMapping("/monthly")
    public String monthlyReport(
            @RequestParam(required = false) Integer year,
            Model model) {

        if (year == null) year = LocalDate.now().getYear();

        List<ReportService.MonthlyRow> rows = reportService.getMonthlyReport(year);

        BigDecimal yearTotal = rows.stream()
                .map(ReportService.MonthlyRow::sales)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal yearGst = rows.stream()
                .map(ReportService.MonthlyRow::gst)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Pre-calculate active months count — no lambda in Thymeleaf
        long activeMonths = rows.stream()
                .filter(r -> r.billCount() > 0)
                .count();

        // Pre-calculate bar widths as 0-100 integers
        // Each bar = (month sales / year total) × 100
        // ✅ Same fix for monthly page bar widths
        List<Integer> barWidths = rows.stream()
                .map(row -> yearTotal.compareTo(BigDecimal.ZERO) > 0
                        && row.sales().compareTo(BigDecimal.ZERO) > 0
                        ? row.sales()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(yearTotal, 0, RoundingMode.HALF_UP)
                        .intValue()
                        : 0)
                .toList();

        model.addAttribute("rows",         rows);
        model.addAttribute("year",         year);
        model.addAttribute("prevYear",     year - 1);
        model.addAttribute("nextYear",     year + 1);
        model.addAttribute("yearTotal",    yearTotal);
        model.addAttribute("yearGst",      yearGst);
        model.addAttribute("activeMonths", activeMonths);
        model.addAttribute("barWidths",    barWidths);
        model.addAttribute("activePage",   "reports");
        model.addAttribute("pageTitle",    "Monthly Report — " + year);

        return "report/monthly";
    }

    // ─────────────────────────────────────────────────────
    // SALES REPORT EXPORTS (DAILY & MONTHLY)
    // ─────────────────────────────────────────────────────
    @GetMapping("/daily/export/csv")
    public ResponseEntity<byte[]> exportDailyCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        if (date == null) date = LocalDate.now();
        ReportService.DailyReport report = reportService.getDailyReport(date);
        
        StringWriter sw = new StringWriter();
        sw.write("Bill No,Date,Customer,Total Amount\n");
        for (Bill b : report.bills()) {
            sw.write(escape(b.getBillNo())); sw.write(",");
            sw.write(b.getBillDate().toString()); sw.write(",");
            sw.write(escape(b.getCustomer() != null ? b.getCustomer().getName() : "Walk-in")); sw.write(",");
            sw.write(fmtCsv(b.getTotalAmount())); sw.write("\n");
        }
        
        byte[] bytes = sw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Daily_Sales_" + date + ".csv\"")
                .body(bytes);
    }

    @GetMapping("/daily/export/excel")
    public ResponseEntity<byte[]> exportDailyExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            if (date == null) date = LocalDate.now();
            ReportService.DailyReport report = reportService.getDailyReport(date);
            byte[] bytes = excelExportService.generateDailyExcel(report.bills(), date.toString());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Daily_Sales_" + date + ".xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/monthly/export/csv")
    public ResponseEntity<byte[]> exportMonthlyCsv(@RequestParam(required = false) Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        List<ReportService.MonthlyRow> rows = reportService.getMonthlyReport(year);
        
        StringWriter sw = new StringWriter();
        sw.write("Month,Bills Count,Sales (Rs),GST Collected (Rs)\n");
        for (ReportService.MonthlyRow row : rows) {
            sw.write(row.monthName() + ",");
            sw.write(row.billCount() + ",");
            sw.write(fmtCsv(row.sales()) + ",");
            sw.write(fmtCsv(row.gst()) + "\n");
        }
        
        byte[] bytes = sw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Monthly_Sales_" + year + ".csv\"")
                .body(bytes);
    }

    @GetMapping("/monthly/export/excel")
    public ResponseEntity<byte[]> exportMonthlyExcel(@RequestParam(required = false) Integer year) {
        try {
            if (year == null) year = LocalDate.now().getYear();
            List<ReportService.MonthlyRow> rows = reportService.getMonthlyReport(year);
            byte[] bytes = excelExportService.generateMonthlyExcel(rows, year);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Monthly_Sales_" + year + ".xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────────────
    // GST REPORT
    // ─────────────────────────────────────────────────────
    @GetMapping("/gst")
    public String gstReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Model model) {

        if (start == null) start = LocalDate.now().withDayOfMonth(1);
        if (end   == null) end   = LocalDate.now();

        ReportService.GstReport report = reportService.getGstReport(start, end);

        // ✅ Pre-calculate grand total
        BigDecimal gstGrandTotal = report.totalTaxable().add(report.totalGst());

        // ✅ Pre-calculate CGST and SGST per bill — Map<billId, halfGst>
        // This replaces the T() division expression in HTML
        Map<Long, BigDecimal> billCgstMap = new java.util.HashMap<>();
        Map<Long, BigDecimal> billSgstMap = new java.util.HashMap<>();

        for (com.example.grocery_billing.entity.Bill bill : report.bills()) {
            BigDecimal gstAmt  = bill.getGstAmount() != null
                    ? bill.getGstAmount() : BigDecimal.ZERO;
            BigDecimal halfGst = gstAmt.divide(
                    BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            billCgstMap.put(bill.getId(), halfGst);
            billSgstMap.put(bill.getId(), halfGst);
        }

        model.addAttribute("report",        report);
        model.addAttribute("gstGrandTotal", gstGrandTotal);
        model.addAttribute("billCgstMap",   billCgstMap);  // ✅ NEW
        model.addAttribute("billSgstMap",   billSgstMap);  // ✅ NEW
        model.addAttribute("start",         start);
        model.addAttribute("end",           end);
        model.addAttribute("activePage",    "reports");
        model.addAttribute("pageTitle",     "GST Report");

        return "report/gst";
    }
    // ─────────────────────────────────────────────────────
    // GST CSV EXPORT
    // ─────────────────────────────────────────────────────
    @GetMapping("/gst/export/csv")
    public ResponseEntity<byte[]> exportGstCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        if (start == null) start = LocalDate.now().withDayOfMonth(1);
        if (end   == null) end   = LocalDate.now();

        ReportService.GstReport report = reportService.getGstReport(start, end);

        StringWriter sw = new StringWriter();
        sw.write("Bill No,Date,Customer,Taxable Amount,CGST,SGST,Grand Total\n");

        for (Bill bill : report.bills()) {
            sw.write(escape(bill.getBillNo())); sw.write(",");
            sw.write(bill.getBillDate().toString()); sw.write(",");
            sw.write(escape(bill.getCustomer() != null ? bill.getCustomer().getName() : "Walk-in")); sw.write(",");
            sw.write(fmtCsv(bill.getSubtotal())); sw.write(",");
            
            BigDecimal gstAmt = bill.getGstAmount() != null ? bill.getGstAmount() : BigDecimal.ZERO;
            BigDecimal halfGst = gstAmt.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            
            sw.write(fmtCsv(halfGst)); sw.write(",");
            sw.write(fmtCsv(halfGst)); sw.write(",");
            sw.write(fmtCsv(bill.getTotalAmount()));
            sw.write("\n");
        }

        byte[] bytes = sw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String filename = "GST_Report_" + start + "_to_" + end + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @GetMapping("/gst/export/excel")
    public ResponseEntity<byte[]> exportGstExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        try {
            if (start == null) start = LocalDate.now().withDayOfMonth(1);
            if (end   == null) end   = LocalDate.now();
            ReportService.GstReport report = reportService.getGstReport(start, end);
            byte[] bytes = excelExportService.generateGstExcel(report.bills(), start.toString(), end.toString());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"GST_Report_" + start + "_to_" + end + ".xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────────────
    // CREDIT REPORT
    // ─────────────────────────────────────────────────────
    @GetMapping("/credit")
    public String creditReport(Model model) {
        ReportService.CreditReport report = reportService.getCreditReport();

        model.addAttribute("report",     report);
        model.addAttribute("activePage", "reports");
        model.addAttribute("pageTitle",  "Credit & Dues Report");

        return "report/credit";
    }

    // ─────────────────────────────────────────────────────
    // CREDIT REPORT — EXCEL EXPORT
    // ─────────────────────────────────────────────────────
    @GetMapping("/credit/export/excel")
    public ResponseEntity<byte[]> exportCreditExcel() {
        try {
            ReportService.CreditReport report = reportService.getCreditReport();
            List<com.example.grocery_billing.entity.Customer> customers = report.customersWithDues();

            org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Credit & Dues Report");

            // Header style
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.RED.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);

            // Title row
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(0);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Credit & Dues Report — Generated: " + java.time.LocalDate.now());
            org.apache.poi.ss.usermodel.CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

            // Summary row
            org.apache.poi.ss.usermodel.Row summaryRow = sheet.createRow(1);
            summaryRow.createCell(0).setCellValue("Total Pending Dues:");
            summaryRow.createCell(1).setCellValue(report.totalDues() != null ? report.totalDues().doubleValue() : 0);
            summaryRow.createCell(2).setCellValue("Customers with dues:");
            summaryRow.createCell(3).setCellValue(customers.size());

            // Blank row
            sheet.createRow(2);

            // Header row
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(3);
            String[] headers = {"#", "Customer Name", "Phone", "Total Credit (₹)", "Total Paid (₹)", "Pending Balance (₹)"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            org.apache.poi.ss.usermodel.CellStyle amountStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.DataFormat format = workbook.createDataFormat();
            amountStyle.setDataFormat(format.getFormat("#,##0.00"));

            org.apache.poi.ss.usermodel.CellStyle dangerStyle = workbook.createCellStyle();
            dangerStyle.setDataFormat(format.getFormat("#,##0.00"));
            org.apache.poi.ss.usermodel.Font dangerFont = workbook.createFont();
            dangerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.RED.getIndex());
            dangerFont.setBold(true);
            dangerStyle.setFont(dangerFont);

            int rowNum = 4;
            int idx = 1;
            for (com.example.grocery_billing.entity.Customer c : customers) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(idx++);
                row.createCell(1).setCellValue(c.getName() != null ? c.getName() : "");
                row.createCell(2).setCellValue(c.getPhone() != null ? c.getPhone() : "");

                org.apache.poi.ss.usermodel.Cell creditCell = row.createCell(3);
                creditCell.setCellValue(c.getTotalCredit() != null ? c.getTotalCredit().doubleValue() : 0);
                creditCell.setCellStyle(amountStyle);

                org.apache.poi.ss.usermodel.Cell paidCell = row.createCell(4);
                paidCell.setCellValue(c.getTotalPaid() != null ? c.getTotalPaid().doubleValue() : 0);
                paidCell.setCellStyle(amountStyle);

                org.apache.poi.ss.usermodel.Cell balCell = row.createCell(5);
                balCell.setCellValue(c.getBalance() != null ? c.getBalance().doubleValue() : 0);
                balCell.setCellStyle(dangerStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            workbook.write(bos);
            workbook.close();

            String filename = "CreditReport_" + java.time.LocalDate.now() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bos.toByteArray());
        } catch (Exception e) {
            log.error("Error exporting credit report to Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────────────
    // CREDIT REPORT — CSV EXPORT
    // ─────────────────────────────────────────────────────
    @GetMapping("/credit/export/csv")
    public ResponseEntity<byte[]> exportCreditCsv() {
        try {
            ReportService.CreditReport report = reportService.getCreditReport();
            List<com.example.grocery_billing.entity.Customer> customers = report.customersWithDues();

            StringWriter sw = new StringWriter();
            sw.write("Credit & Dues Report - Generated: " + java.time.LocalDate.now() + "\n");
            sw.write("Total Pending Dues:," + (report.totalDues() != null ? report.totalDues() : "0.00") + "\n\n");
            sw.write("#,Customer Name,Phone,Total Credit (Rs),Total Paid (Rs),Pending Balance (Rs)\n");

            int idx = 1;
            for (com.example.grocery_billing.entity.Customer c : customers) {
                sw.write(idx++ + ",");
                sw.write(escape(c.getName()) + ",");
                sw.write(escape(c.getPhone()) + ",");
                sw.write(fmtCsv(c.getTotalCredit()) + ",");
                sw.write(fmtCsv(c.getTotalPaid()) + ",");
                sw.write(fmtCsv(c.getBalance()) + "\n");
            }

            byte[] bytes = sw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            String filename = "CreditReport_" + java.time.LocalDate.now() + ".csv";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(bytes);
        } catch (Exception e) {
            log.error("Error exporting credit report to CSV", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────────────
    // CSV EXPORT
    // GET /reports/export/csv?start=2024-01-01&end=2024-01-31
    // ─────────────────────────────────────────────────────
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        if (start == null) start = LocalDate.now().withDayOfMonth(1);
        if (end   == null) end   = LocalDate.now();

        List<Bill> bills = reportService.getBillsForExport(start, end);

        String csv   = buildCsv(bills);
        byte[] bytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String filename = "Bills-" + start + "-to-" + end + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportBillsExcel(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        try {
            if (start == null) start = LocalDate.now().withDayOfMonth(1);
            if (end   == null) end   = LocalDate.now();
            List<Bill> bills = reportService.getBillsForExport(start, end);
            byte[] bytes = excelExportService.generateAllBillsExcel(bills, start.toString(), end.toString());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Bills_" + start + "_to_" + end + ".xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────────────
    // BUILD CSV STRING
    // ─────────────────────────────────────────────────────
    private String buildCsv(List<Bill> bills) {
        StringWriter sw = new StringWriter();

        sw.write("Bill No,Date,Customer,Payment Method,Payment Status," +
                "Subtotal,GST Amount,Transport,Extra,Discount,Total Amount\n");

        for (Bill b : bills) {
            sw.write(escape(b.getBillNo()));                               sw.write(",");
            sw.write(b.getBillDate().toString());                          sw.write(",");
            sw.write(escape(b.getCustomer() != null
                    ? b.getCustomer().getName() : "Walk-in"));             sw.write(",");
            sw.write(escape(b.getPaymentMethod() != null
                    ? b.getPaymentMethod() : "CASH"));                     sw.write(",");
            sw.write(escape(b.getPaymentStatus().name()));                 sw.write(",");
            sw.write(fmtCsv(b.getSubtotal()));                            sw.write(",");
            sw.write(fmtCsv(b.getGstAmount()));                           sw.write(",");
            sw.write(fmtCsv(b.getTransportCost()));                       sw.write(",");
            sw.write(fmtCsv(b.getExtraCost()));                           sw.write(",");
            sw.write(fmtCsv(b.getDiscount()));                            sw.write(",");
            sw.write(fmtCsv(b.getTotalAmount()));
            sw.write("\n");
        }

        return sw.toString();
    }

    private String escape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private String fmtCsv(BigDecimal val) {
        return val != null ? String.format("%.2f", val) : "0.00";
    }
    // ── Inject ProfitReportService ────────────────────────
// Add to constructor / @RequiredArgsConstructor fields:
    private final ProfitReportService profitReportService;

    // ── PROFIT & LOSS PAGE ────────────────────────────────
    @GetMapping("/profit")
    public String profitReport(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model) {

        LocalDate now = LocalDate.now();
        if (month == null) month = now.getMonthValue();
        if (year  == null) year  = now.getYear();

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.withDayOfMonth(
                start.lengthOfMonth());

        ProfitReportService.ProfitReport report =
                profitReportService.generateReport(start, end);

        model.addAttribute("report",    report);
        model.addAttribute("month",     month);
        model.addAttribute("year",      year);
        model.addAttribute("monthName",
                start.format(java.time.format.DateTimeFormatter
                        .ofPattern("MMMM yyyy")));
        model.addAttribute("activePage", "reports");
        model.addAttribute("pageTitle",  "Profit & Loss");
        return "report/profit";
    }
    @GetMapping("/profit/download")
    public ResponseEntity<byte[]> downloadProfitExcel(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        try {
            LocalDate now = LocalDate.now();
            if (month == null) month = now.getMonthValue();
            if (year  == null) year  = now.getYear();

            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end   = start.withDayOfMonth(
                    start.lengthOfMonth());

            String monthName = start.format(
                    java.time.format.DateTimeFormatter
                            .ofPattern("MMMM_yyyy"));

            ProfitReportService.ProfitReport report =
                    profitReportService.generateReport(
                            start, end);

            byte[] excel = profitReportService
                    .generateProfitExcel(report, monthName);

            String filename = "ProfitLoss_"
                    + monthName + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-"
                            + "officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename(filename).build());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excel);

        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError().build();
        }
    }

    @GetMapping("/profit/export/csv")
    public ResponseEntity<byte[]> exportProfitCsv(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        try {
            LocalDate now = LocalDate.now();
            if (month == null) month = now.getMonthValue();
            if (year  == null) year  = now.getYear();
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());
            ProfitReportService.ProfitReport report = profitReportService.generateReport(start, end);
            byte[] csv = profitReportService.generateCsv(report);
            String monthName = start.format(java.time.format.DateTimeFormatter.ofPattern("MMMM_yyyy"));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ProfitLoss_" + monthName + ".csv\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
