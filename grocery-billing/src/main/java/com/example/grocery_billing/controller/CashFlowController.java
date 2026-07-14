package com.example.grocery_billing.controller;

import com.example.grocery_billing.entity.CashFlow;
import com.example.grocery_billing.service.CashFlowService;
import com.example.grocery_billing.service.ExcelCashFlowService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/cashflow")
@RequiredArgsConstructor
public class CashFlowController {

    private final CashFlowService cashFlowService;
    private final ExcelCashFlowService excelCashFlowService;

    @GetMapping
    public String index(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Model model) {

        if (start == null) {
            start = LocalDate.now().withDayOfMonth(1);
        }
        if (end == null) {
            end = LocalDate.now();
        }

        List<CashFlow> entries = cashFlowService.getEntriesForDateRange(start, end);
        CashFlowService.CashFlowSummary summary = cashFlowService.getSummaryForDateRange(start, end);

        model.addAttribute("entries", entries);
        model.addAttribute("summary", summary);
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        model.addAttribute("activePage", "cashflow");
        model.addAttribute("pageTitle", "Cash Flow Tracking");

        return "cashflow/index";
    }

    @PostMapping("/add")
    public String addEntry(@ModelAttribute CashFlow cashFlow, RedirectAttributes ra) {
        try {
            cashFlowService.addEntry(cashFlow);
            ra.addFlashAttribute("successMessage", "Transaction added successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error adding transaction: " + e.getMessage());
        }
        return "redirect:/cashflow";
    }

    @PostMapping("/delete/{id}")
    public String deleteEntry(@PathVariable Long id, RedirectAttributes ra) {
        try {
            cashFlowService.deleteEntry(id);
            ra.addFlashAttribute("successMessage", "Transaction deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error deleting transaction.");
        }
        return "redirect:/cashflow";
    }

    @GetMapping("/export")
    public void exportToExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            HttpServletResponse response) throws Exception {

        if (start == null) {
            start = LocalDate.now().withDayOfMonth(1);
        }
        if (end == null) {
            end = LocalDate.now();
        }

        List<CashFlow> entries = cashFlowService.getEntriesForDateRange(start, end);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"cashflow_ledger.xlsx\"");

        excelCashFlowService.exportToExcel(entries, response.getOutputStream());
    }
}
