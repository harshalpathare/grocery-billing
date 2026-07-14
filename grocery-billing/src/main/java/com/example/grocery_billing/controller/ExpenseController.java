package com.example.grocery_billing.controller;

import com.example.grocery_billing.entity.Expense;
import com.example.grocery_billing.service.ExcelExpenseReportService;
import com.example.grocery_billing.service.ExpenseService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ExcelExpenseReportService excelExpenseReportService;

    @GetMapping
    public String listExpenses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Model model) {

        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        List<Expense> expenses = expenseService.getExpensesByCategoryAndDate(category, startDate, endDate);
        
        BigDecimal totalAmount = expenses.stream()
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("expenses", expenses);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("categories", expenseService.getExpenseCategories());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        
        if (!model.containsAttribute("expense")) {
            Expense newExpense = new Expense();
            newExpense.setExpenseDate(LocalDate.now());
            model.addAttribute("expense", newExpense);
        }

        model.addAttribute("activePage", "expenses");
        model.addAttribute("pageTitle", "Expenses");
        return "expenses/list";
    }

    @PostMapping("/save")
    public String saveExpense(@Valid @ModelAttribute("expense") Expense expense,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            result.getFieldErrors().forEach(e -> log.error("Validation error in field '{}': {}", e.getField(), e.getDefaultMessage()));
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.expense", result);
            redirectAttributes.addFlashAttribute("expense", expense);
            redirectAttributes.addFlashAttribute("error", "Please correct the errors below.");
            return "redirect:/expenses";
        }

        try {
            expenseService.saveExpense(expense);
            redirectAttributes.addFlashAttribute("success", "Expense added successfully!");
        } catch (Exception e) {
            log.error("Error saving expense", e);
            redirectAttributes.addFlashAttribute("error", "Error saving expense: " + e.getMessage());
        }
        return "redirect:/expenses";
    }

    @GetMapping("/delete/{id}")
    public String deleteExpense(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            expenseService.deleteExpense(id);
            redirectAttributes.addFlashAttribute("success", "Expense deleted successfully.");
        } catch (Exception e) {
            log.error("Error deleting expense", e);
            redirectAttributes.addFlashAttribute("error", "Error deleting expense: " + e.getMessage());
        }
        return "redirect:/expenses";
    }

    @GetMapping("/export")
    public void exportExpenses(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            HttpServletResponse response) throws IOException {

        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"expenses_report.xlsx\"");
        
        excelExpenseReportService.exportExpenseReport(startDate, endDate, category, response.getOutputStream());
    }

    @PostMapping("/bulk-delete")
    public String bulkDelete(@RequestParam("ids") java.util.List<Long> ids, org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        try {
            for (Long id : ids) {
                expenseService.deleteExpense(id);
            }
            ra.addFlashAttribute("successMessage", "Selected expenses deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Error deleting expenses: " + e.getMessage());
        }
        return "redirect:/expenses";
    }

}
