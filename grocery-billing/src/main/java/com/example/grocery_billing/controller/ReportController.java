package com.example.grocery_billing.controller;

import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

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
                .contentLength(bytes.length)
                .body(bytes);
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
}
