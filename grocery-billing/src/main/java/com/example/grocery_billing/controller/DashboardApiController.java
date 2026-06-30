package com.example.grocery_billing.controller;

import com.example.grocery_billing.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for dashboard charts.
 * Returns last 7 days sales data for Chart.js.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {

    private final BillRepository billRepository;

    // GET /api/dashboard/sales-chart
    @GetMapping("/sales-chart")
    public List<Map<String, Object>> getSalesChart() {
        List<Map<String, Object>> data = new ArrayList<>();
        LocalDate today = LocalDate.now();

        DateTimeFormatter labelFmt  = DateTimeFormatter.ofPattern("dd MMM");

        for (int i = 6; i >= 0; i--) {
            LocalDate date   = today.minusDays(i);
            BigDecimal sales = billRepository.getTotalSalesByDate(date);
            if (sales == null) sales = BigDecimal.ZERO;

            Map<String, Object> point = new HashMap<>();
            point.put("date",   date.format(labelFmt));
            point.put("amount", sales.doubleValue());
            point.put("rawDate", date.toString());
            data.add(point);
        }

        return data;
    }
}
