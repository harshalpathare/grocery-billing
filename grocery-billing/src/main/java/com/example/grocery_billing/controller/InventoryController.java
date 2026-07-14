package com.example.grocery_billing.controller;

import com.example.grocery_billing.entity.Product;
import com.example.grocery_billing.service.ExcelInventoryReportService;
import com.example.grocery_billing.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final ExcelInventoryReportService excelInventoryReportService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("summary", inventoryService.getInventorySummary());
        model.addAttribute("lowStockAlerts", inventoryService.getLowStockAlerts());
        model.addAttribute("outOfStockAlerts", inventoryService.getOutOfStockAlerts());
        model.addAttribute("expiryAlerts", inventoryService.getExpiringProducts(30));

        model.addAttribute("activePage", "inventory");
        model.addAttribute("pageTitle", "Inventory Dashboard");
        return "inventory/dashboard";
    }

    @GetMapping("/export")
    public void exportInventory(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"inventory_report.xlsx\"");
        excelInventoryReportService.exportInventory(response.getOutputStream());
    }

    @GetMapping("/reports/stock")
    public String stockReport(Model model) {
        model.addAttribute("products", inventoryService.getAllStock());
        model.addAttribute("reportType", "Stock Report");
        model.addAttribute("activePage", "inventory");
        return "inventory/reports";
    }

    @GetMapping("/reports/dead")
    public String deadStockReport(Model model) {
        model.addAttribute("products", inventoryService.getDeadStock(90)); // 90 days
        model.addAttribute("reportType", "Dead Stock (No Sales in 90 Days)");
        model.addAttribute("activePage", "inventory");
        return "inventory/reports";
    }

    @GetMapping("/reports/fast")
    public String fastMovingReport(Model model) {
        model.addAttribute("productSales", inventoryService.getFastMovingProducts(30));
        model.addAttribute("reportType", "Fast Moving Products (Last 30 Days)");
        model.addAttribute("activePage", "inventory");
        return "inventory/reports";
    }

    @GetMapping("/reports/slow")
    public String slowMovingReport(Model model) {
        model.addAttribute("productSales", inventoryService.getSlowMovingProducts(30));
        model.addAttribute("reportType", "Slow Moving Products (Last 30 Days)");
        model.addAttribute("activePage", "inventory");
        return "inventory/reports";
    }

    @GetMapping("/reports/expiry")
    public String expiryReport(Model model) {
        model.addAttribute("products", inventoryService.getExpiringProducts(30));
        model.addAttribute("reportType", "Expiry Report (Next 30 Days)");
        model.addAttribute("activePage", "inventory");
        return "inventory/reports";
    }
}
