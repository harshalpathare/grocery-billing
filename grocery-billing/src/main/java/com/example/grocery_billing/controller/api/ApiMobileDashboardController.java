package com.example.grocery_billing.controller.api;

import com.example.grocery_billing.repository.BillRepository;
import com.example.grocery_billing.repository.CustomerRepository;
import com.example.grocery_billing.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class ApiMobileDashboardController {

    private final BillRepository billRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        LocalDate today = LocalDate.now();
        BigDecimal todaySales = billRepository.getTotalSalesByDate(today);
        if (todaySales == null) {
            todaySales = BigDecimal.ZERO;
        }

        long totalCustomers = customerRepository.count();
        long totalProducts = productRepository.count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("todaySales", todaySales);
        summary.put("totalCustomers", totalCustomers);
        summary.put("totalProducts", totalProducts);
        
        return ResponseEntity.ok(summary);
    }
}
