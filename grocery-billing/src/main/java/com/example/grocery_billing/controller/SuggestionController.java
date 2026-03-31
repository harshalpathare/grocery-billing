package com.example.grocery_billing.controller;

import com.example.grocery_billing.service.SmartSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final SmartSuggestionService suggestionService;

    // Get suggestions for customer
    @GetMapping("/customer/{customerId}")
    public List<Map<String, Object>> forCustomer(
            @PathVariable Long customerId,
            @RequestParam(required = false)
            List<Long> currentProducts) {
        return suggestionService
                .getSuggestionsForCustomer(
                        customerId, currentProducts);
    }

    // Get frequently bought together
    @PostMapping("/together")
    public List<Map<String, Object>> together(
            @RequestBody Map<String, List<Long>> body) {
        return suggestionService
                .getFrequentlyBoughtTogether(
                        body.get("productIds"));
    }

    // Get popular products
    @PostMapping("/popular")
    public List<Map<String, Object>> popular(
            @RequestBody Map<String, List<Long>> body) {
        return suggestionService
                .getPopularProducts(body.get("excludeIds"));
    }
}