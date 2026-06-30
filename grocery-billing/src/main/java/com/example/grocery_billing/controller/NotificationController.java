package com.example.grocery_billing.controller;

import com.example.grocery_billing.entity.Customer;
import com.example.grocery_billing.entity.Product;
import com.example.grocery_billing.service.CustomerService;
import com.example.grocery_billing.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for notification bell data.
 * Returns low stock + pending credit info as JSON.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController {

    private final ProductService  productService;
    private final CustomerService customerService;

    // GET /api/notifications
    @GetMapping("/notifications")
    public Map<String, Object> getNotifications() {
        Map<String, Object> result = new HashMap<>();

        // ── Low Stock Products (stock <= 5) ───────────────────
        List<Product> lowStock = productService.getAllActiveProducts()
                .stream()
            .filter(p -> p.getStockQty() != null
                && p.getStockQty().compareTo(BigDecimal.ZERO) >= 0
                && p.getStockQty().compareTo(BigDecimal.valueOf(5)) <= 0)
                .collect(Collectors.toList());

        List<Map<String, Object>> lowStockItems = lowStock.stream().map(p -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name",  p.getNameEn());
            item.put("stock", p.getStockQty() != null
                    ? p.getStockQty().stripTrailingZeros().toPlainString() : "0");
            item.put("unit",  p.getUnit() != null ? p.getUnit() : "piece");
            return item;
        }).collect(Collectors.toList());

        // ── Pending Credit Customers ──────────────────────────
        List<Customer> creditCustomers = customerService.getCustomersWithPendingBalance();

        List<Map<String, Object>> creditItems = creditCustomers.stream()
                .limit(10) // cap at 10 in dropdown
                .map(c -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name",    c.getName());
                    item.put("balance", c.getBalance() != null
                            ? c.getBalance().setScale(2, RoundingMode.HALF_UP).toPlainString()
                            : "0.00");
                    return item;
                }).collect(Collectors.toList());

        result.put("lowStockCount", lowStockItems.size());
        result.put("creditCount",   creditItems.size());
        result.put("lowStockItems", lowStockItems);
        result.put("creditItems",   creditItems);

        return result;
    }
}
