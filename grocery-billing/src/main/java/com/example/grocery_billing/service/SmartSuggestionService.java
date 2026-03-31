package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.entity.BillItem;
import com.example.grocery_billing.entity.Customer;
import com.example.grocery_billing.repository.BillRepository;
import com.example.grocery_billing.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartSuggestionService {

    private final BillRepository     billRepository;
    private final CustomerRepository customerRepository;

    // ─────────────────────────────────────────────
    // 1. GET SUGGESTIONS FOR CUSTOMER
    // Based on their past purchase history
    // ─────────────────────────────────────────────
    public List<Map<String, Object>> getSuggestionsForCustomer(
            Long customerId,
            List<Long> currentProductIds) {

        try {
            // Get customer's last 20 bills
            List<Bill> customerBills = billRepository
                    .findAll().stream()
                    .filter(b -> b.getCustomer() != null
                            && b.getCustomer().getId()
                            .equals(customerId))
                    .sorted(Comparator.comparing(
                            Bill::getBillDate).reversed())
                    .limit(20)
                    .collect(Collectors.toList());

            if (customerBills.isEmpty()) {
                return getPopularProducts(currentProductIds);
            }

            // Count product frequency
            Map<Long, Integer>   productCount = new HashMap<>();
            Map<Long, String>    productNames = new HashMap<>();
            Map<Long, Double>    productPrice = new HashMap<>();

            for (Bill bill : customerBills) {
                for (BillItem item : bill.getBillItems()) {
                    Long pid = item.getProduct().getId();
                    productCount.merge(pid, 1, Integer::sum);
                    productNames.put(pid,
                            item.getProductNameSnapshot() != null
                                    ? item.getProductNameSnapshot()
                                    : item.getProduct().getNameEn());
                    productPrice.put(pid,
                            item.getUnitPrice().doubleValue());
                }
            }

            // Remove already added products
            if (currentProductIds != null) {
                currentProductIds.forEach(productCount::remove);
            }

            // Return top 5 suggestions
            return productCount.entrySet().stream()
                    .sorted(Map.Entry.<Long, Integer>
                            comparingByValue().reversed())
                    .limit(5)
                    .map(e -> {
                        Map<String, Object> m =
                                new LinkedHashMap<>();
                        m.put("productId",   e.getKey());
                        m.put("productName",
                                productNames.get(e.getKey()));
                        m.put("price",
                                productPrice.get(e.getKey()));
                        m.put("buyCount",    e.getValue());
                        m.put("reason",
                                "Bought " + e.getValue()
                                        + " time(s) before");
                        return m;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting suggestions: {}",
                    e.getMessage());
            return new ArrayList<>();
        }
    }

    // ─────────────────────────────────────────────
    // 2. FREQUENTLY BOUGHT TOGETHER
    // Products often bought with current cart items
    // ─────────────────────────────────────────────
    public List<Map<String, Object>> getFrequentlyBoughtTogether(
            List<Long> currentProductIds) {

        if (currentProductIds == null
                || currentProductIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Integer>  coCount  = new HashMap<>();
        Map<Long, String>   coNames  = new HashMap<>();
        Map<Long, Double>   coPrices = new HashMap<>();

        List<Bill> recentBills = billRepository
                .findAll().stream()
                .sorted(Comparator.comparing(
                        Bill::getBillDate).reversed())
                .limit(100)
                .collect(Collectors.toList());

        for (Bill bill : recentBills) {
            List<Long> billProductIds = bill.getBillItems()
                    .stream()
                    .map(i -> i.getProduct().getId())
                    .collect(Collectors.toList());

            // Check if bill contains any current product
            boolean hasCurrentProduct = currentProductIds
                    .stream()
                    .anyMatch(billProductIds::contains);

            if (hasCurrentProduct) {
                // Count other products in same bill
                for (BillItem item : bill.getBillItems()) {
                    Long pid = item.getProduct().getId();
                    if (!currentProductIds.contains(pid)) {
                        coCount.merge(pid, 1, Integer::sum);
                        coNames.put(pid,
                                item.getProductNameSnapshot()
                                        != null
                                        ? item.getProductNameSnapshot()
                                        : item.getProduct().getNameEn());
                        coPrices.put(pid,
                                item.getUnitPrice().doubleValue());
                    }
                }
            }
        }

        return coCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>
                        comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("productId",   e.getKey());
                    m.put("productName", coNames.get(e.getKey()));
                    m.put("price",       coPrices.get(e.getKey()));
                    m.put("coCount",     e.getValue());
                    m.put("reason",
                            "Often bought together ("
                                    + e.getValue() + " times)");
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // 3. POPULAR PRODUCTS (fallback)
    // ─────────────────────────────────────────────
    public List<Map<String, Object>> getPopularProducts(
            List<Long> excludeIds) {

        Map<Long, Integer> productCount  = new HashMap<>();
        Map<Long, String>  productNames  = new HashMap<>();
        Map<Long, Double>  productPrices = new HashMap<>();

        billRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        Bill::getBillDate).reversed())
                .limit(50)
                .forEach(bill -> {
                    for (BillItem item : bill.getBillItems()) {
                        Long pid = item.getProduct().getId();
                        productCount.merge(pid, 1, Integer::sum);
                        productNames.put(pid,
                                item.getProductNameSnapshot()
                                        != null
                                        ? item.getProductNameSnapshot()
                                        : item.getProduct().getNameEn());
                        productPrices.put(pid,
                                item.getUnitPrice().doubleValue());
                    }
                });

        if (excludeIds != null) {
            excludeIds.forEach(productCount::remove);
        }

        return productCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>
                        comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("productId",   e.getKey());
                    m.put("productName", productNames.get(e.getKey()));
                    m.put("price",       productPrices.get(e.getKey()));
                    m.put("buyCount",    e.getValue());
                    m.put("reason",      "Popular product");
                    return m;
                })
                .collect(Collectors.toList());
    }
}