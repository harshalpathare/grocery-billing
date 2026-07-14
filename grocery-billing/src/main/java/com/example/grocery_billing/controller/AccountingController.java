package com.example.grocery_billing.controller;

import com.example.grocery_billing.dto.JournalEntryDto;
import com.example.grocery_billing.service.AccountingService;
import com.example.grocery_billing.service.ExcelAccountingService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final AccountingService accountingService;
    private final ExcelAccountingService excelAccountingService;

    private void prepareDateRange(Model model, LocalDate start, LocalDate end) {
        if (start == null) start = LocalDate.now().withDayOfMonth(1);
        if (end == null) end = LocalDate.now();
        model.addAttribute("start", start);
        model.addAttribute("end", end);
    }

    private LocalDate safeStart(LocalDate start) {
        return start == null ? LocalDate.now().withDayOfMonth(1) : start;
    }

    private LocalDate safeEnd(LocalDate end) {
        return end == null ? LocalDate.now() : end;
    }

    // ── DASHBOARD ─────────────────────────────────────────
    @GetMapping({"", "/", "/dashboard"})
    public String viewDashboard(Model model) {
        model.addAttribute("activePage", "accounting");
        return "accounting/dashboard";
    }

    // ── JOURNAL ───────────────────────────────────────────
    @GetMapping("/journal")
    public String viewJournal(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Model model) {
        prepareDateRange(model, start, end);
        model.addAttribute("entries", accountingService.generateJournal(safeStart(start), safeEnd(end)));
        model.addAttribute("activePage", "accounting");
        return "accounting/journal";
    }

    @GetMapping("/journal/export")
    public void exportJournal(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            HttpServletResponse response) throws Exception {
        
        List<JournalEntryDto> entries = accountingService.generateJournal(safeStart(start), safeEnd(end));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"journal.xlsx\"");
        excelAccountingService.exportJournal(entries, response.getOutputStream());
    }

    // ── LEDGER ────────────────────────────────────────────
    @GetMapping("/ledger")
    public String viewLedger(
            @RequestParam(required = false) String account,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Model model) {
        prepareDateRange(model, start, end);
        model.addAttribute("accounts", accountingService.getAllAccountNames(safeStart(start), safeEnd(end)));
        model.addAttribute("selectedAccount", account);
        if (account != null && !account.isBlank()) {
            model.addAttribute("entries", accountingService.getLedger(account, safeStart(start), safeEnd(end)));
        }
        model.addAttribute("activePage", "accounting");
        return "accounting/ledger";
    }

    @GetMapping("/ledger/export")
    public void exportLedger(
            @RequestParam String account,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            HttpServletResponse response) throws Exception {
        
        List<JournalEntryDto> entries = accountingService.getLedger(account, safeStart(start), safeEnd(end));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"ledger_" + account + ".xlsx\"");
        excelAccountingService.exportLedger(account, entries, response.getOutputStream());
    }

    // ── CASH BOOK ─────────────────────────────────────────
    @GetMapping("/cashbook")
    public String viewCashBook(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Model model) {
        prepareDateRange(model, start, end);
        model.addAttribute("entries", accountingService.getLedger("Cash", safeStart(start), safeEnd(end)));
        model.addAttribute("accountName", "Cash Book");
        model.addAttribute("activePage", "accounting");
        return "accounting/book_view";
    }

    @GetMapping("/cashbook/export")
    public void exportCashBook(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            HttpServletResponse response) throws Exception {
        
        List<JournalEntryDto> entries = accountingService.getLedger("Cash", safeStart(start), safeEnd(end));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"cashbook.xlsx\"");
        excelAccountingService.exportLedger("Cash", entries, response.getOutputStream());
    }

    // ── BANK BOOK ─────────────────────────────────────────
    @GetMapping("/bankbook")
    public String viewBankBook(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Model model) {
        prepareDateRange(model, start, end);
        model.addAttribute("entries", accountingService.getLedger("Bank", safeStart(start), safeEnd(end)));
        model.addAttribute("accountName", "Bank Book");
        model.addAttribute("activePage", "accounting");
        return "accounting/book_view";
    }

    @GetMapping("/bankbook/export")
    public void exportBankBook(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            HttpServletResponse response) throws Exception {
        
        List<JournalEntryDto> entries = accountingService.getLedger("Bank", safeStart(start), safeEnd(end));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"bankbook.xlsx\"");
        excelAccountingService.exportLedger("Bank", entries, response.getOutputStream());
    }

    // ── TRIAL BALANCE ─────────────────────────────────────
    @GetMapping("/trialbalance")
    public String viewTrialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Model model) {
        prepareDateRange(model, start, end);
        model.addAttribute("tb", accountingService.getTrialBalance(safeStart(start), safeEnd(end)));
        model.addAttribute("activePage", "accounting");
        return "accounting/trial_balance";
    }

    @GetMapping("/trialbalance/export")
    public void exportTrialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            HttpServletResponse response) throws Exception {
        
        Map<String, BigDecimal> tb = accountingService.getTrialBalance(safeStart(start), safeEnd(end));
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"trial_balance.xlsx\"");
        excelAccountingService.exportTrialBalance(tb, response.getOutputStream());
    }
}
