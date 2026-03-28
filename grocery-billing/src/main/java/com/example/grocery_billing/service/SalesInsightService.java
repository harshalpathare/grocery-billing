package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.entity.BillItem;
import com.example.grocery_billing.repository.BillRepository;
import com.example.grocery_billing.repository.BillItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesInsightService {

    private final BillRepository billRepository;

    // ─────────────────────────────────────────────
    // 1. TOP SELLING PRODUCTS
    // ─────────────────────────────────────────────
    public List<Map<String, Object>> getTopSellingProducts(int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        List<Bill> bills = billRepository
                .findByBillDateGreaterThanEqual(from);

        Map<String, Double> productQty = new HashMap<>();
        Map<String, BigDecimal> productRevenue = new HashMap<>();

        for (Bill bill : bills) {
            for (BillItem item : bill.getBillItems()) {
                String name = item.getProductNameSnapshot() != null
                        ? item.getProductNameSnapshot()
                        : item.getProduct().getNameEn();
                productQty.merge(name,
                        item.getQuantity().doubleValue(),
                        Double::sum);
                productRevenue.merge(name,
                        item.getItemTotal(),
                        BigDecimal::add);
            }
        }

        return productQty.entrySet().stream()
                .sorted(Map.Entry.<String, Double>
                        comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("product", e.getKey());
                    m.put("quantity", e.getValue());
                    m.put("revenue",
                            productRevenue.getOrDefault(
                                            e.getKey(), BigDecimal.ZERO)
                                    .setScale(2, RoundingMode.HALF_UP));
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // 2. SLOW MOVING PRODUCTS (need to stock less)
    // ─────────────────────────────────────────────
    public List<Map<String, Object>> getSlowMovingProducts(int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        List<Bill> bills = billRepository
                .findByBillDateGreaterThanEqual(from);

        Map<String, Double> productQty = new HashMap<>();

        for (Bill bill : bills) {
            for (BillItem item : bill.getBillItems()) {
                String name = item.getProductNameSnapshot() != null
                        ? item.getProductNameSnapshot()
                        : item.getProduct().getNameEn();
                productQty.merge(name,
                        item.getQuantity().doubleValue(),
                        Double::sum);
            }
        }

        return productQty.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("product",  e.getKey());
                    m.put("quantity", e.getValue());
                    m.put("status",   "Slow Moving ⚠️");
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // 3. BEST DAYS FOR SALES (promotions timing)
    // ─────────────────────────────────────────────
    public List<Map<String, Object>> getBestSalesDays(int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        List<Bill> bills = billRepository
                .findByBillDateGreaterThanEqual(from);

        Map<DayOfWeek, BigDecimal> dayRevenue  = new HashMap<>();
        Map<DayOfWeek, Integer>    dayCount    = new HashMap<>();

        for (Bill bill : bills) {
            DayOfWeek day = bill.getBillDate().getDayOfWeek();
            dayRevenue.merge(day,
                    bill.getTotalAmount() != null
                            ? bill.getTotalAmount()
                            : BigDecimal.ZERO,
                    BigDecimal::add);
            dayCount.merge(day, 1, Integer::sum);
        }

        return dayRevenue.entrySet().stream()
                .sorted(Map.Entry.<DayOfWeek, BigDecimal>
                        comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("day", e.getKey().getDisplayName(
                            TextStyle.FULL, Locale.ENGLISH));
                    m.put("revenue",   e.getValue()
                            .setScale(2, RoundingMode.HALF_UP));
                    m.put("billCount", dayCount.get(e.getKey()));
                    m.put("avgSale",
                            e.getValue()
                                    .divide(BigDecimal.valueOf(
                                                    dayCount.get(e.getKey())),
                                            2, RoundingMode.HALF_UP));
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // 4. TOP CUSTOMERS (likely to buy again)
    // ─────────────────────────────────────────────
    public List<Map<String, Object>> getTopCustomers(int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        List<Bill> bills = billRepository
                .findByBillDateGreaterThanEqual(from);

        Map<String, BigDecimal> customerSpend = new HashMap<>();
        Map<String, Integer>    customerVisits = new HashMap<>();
        Map<String, LocalDate>  customerLastVisit = new HashMap<>();

        for (Bill bill : bills) {
            if (bill.getCustomer() == null) continue;
            String name = bill.getCustomer().getName();
            customerSpend.merge(name,
                    bill.getTotalAmount() != null
                            ? bill.getTotalAmount()
                            : BigDecimal.ZERO,
                    BigDecimal::add);
            customerVisits.merge(name, 1, Integer::sum);
            customerLastVisit.merge(name,
                    bill.getBillDate(),
                    (a, b) -> a.isAfter(b) ? a : b);
        }

        return customerSpend.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>
                        comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("customer",  e.getKey());
                    m.put("totalSpend", e.getValue()
                            .setScale(2, RoundingMode.HALF_UP));
                    m.put("visits",    customerVisits.get(e.getKey()));
                    m.put("lastVisit", customerLastVisit.get(e.getKey()));
                    m.put("avgPerVisit",
                            e.getValue()
                                    .divide(BigDecimal.valueOf(
                                                    customerVisits.get(e.getKey())),
                                            2, RoundingMode.HALF_UP));
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // 5. MONTHLY SALES TREND
    // ─────────────────────────────────────────────
    public List<Map<String, Object>> getMonthlySalesTrend(int months) {
        LocalDate from = LocalDate.now().minusMonths(months);
        List<Bill> bills = billRepository
                .findByBillDateGreaterThanEqual(from);

        Map<String, BigDecimal> monthRevenue = new LinkedHashMap<>();
        Map<String, Integer>    monthCount   = new LinkedHashMap<>();

        for (Bill bill : bills) {
            String key = bill.getBillDate().getYear() + "-"
                    + String.format("%02d",
                    bill.getBillDate().getMonthValue());
            monthRevenue.merge(key,
                    bill.getTotalAmount() != null
                            ? bill.getTotalAmount()
                            : BigDecimal.ZERO,
                    BigDecimal::add);
            monthCount.merge(key, 1, Integer::sum);
        }

        return monthRevenue.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("month",     e.getKey());
                    m.put("revenue",   e.getValue()
                            .setScale(2, RoundingMode.HALF_UP));
                    m.put("billCount", monthCount.get(e.getKey()));
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // 6. AI RECOMMENDATIONS
    // Generates smart text suggestions based on data
    // ─────────────────────────────────────────────
    public List<String> getAiRecommendations(int days) {
        List<String> recommendations = new ArrayList<>();

        List<Map<String, Object>> topProducts =
                getTopSellingProducts(days);
        List<Map<String, Object>> slowProducts =
                getSlowMovingProducts(days);
        List<Map<String, Object>> bestDays =
                getBestSalesDays(days);
        List<Map<String, Object>> topCustomers =
                getTopCustomers(days);

        // Stock recommendations
        if (!topProducts.isEmpty()) {
            String top = (String) topProducts.get(0).get("product");
            recommendations.add("📦 Stock more of **" + top
                    + "** — it is your best selling product in last "
                    + days + " days.");
        }
        if (topProducts.size() >= 2) {
            String second = (String) topProducts.get(1).get("product");
            recommendations.add("📦 Keep good stock of **" + second
                    + "** — consistently high demand.");
        }

        // Slow moving
        if (!slowProducts.isEmpty()) {
            String slow = (String) slowProducts.get(0).get("product");
            recommendations.add("⚠️ Consider reducing stock of **"
                    + slow + "** — very low sales in last "
                    + days + " days.");
        }

        // Best day for promotions
        if (!bestDays.isEmpty()) {
            String bestDay = (String) bestDays.get(0).get("day");
            Object revenue  = bestDays.get(0).get("revenue");
            recommendations.add("🎯 Best day for promotions: **"
                    + bestDay + "** — highest sales of ₹"
                    + revenue + " on average.");
        }
        if (bestDays.size() >= 2) {
            String secondDay = (String) bestDays.get(1).get("day");
            recommendations.add("🎯 **" + secondDay
                    + "** is also a strong sales day — "
                    + "good time to run offers.");
        }

        // Customer recommendations
        if (!topCustomers.isEmpty()) {
            String topCust = (String) topCustomers.get(0)
                    .get("customer");
            Object spend    = topCustomers.get(0).get("totalSpend");
            recommendations.add("👤 **" + topCust
                    + "** is your best customer with ₹"
                    + spend + " spent — consider giving loyalty discount.");
        }

        // Low sales days
        if (!bestDays.isEmpty()) {
            String worstDay = (String) bestDays
                    .get(bestDays.size() - 1).get("day");
            recommendations.add("💡 Sales are lowest on **"
                    + worstDay + "** — run special offers "
                    + "to attract more customers.");
        }

        if (recommendations.isEmpty()) {
            recommendations.add(
                    "📊 Not enough data yet. "
                            + "Keep billing to get AI insights!");
        }

        return recommendations;
    }

    // ─────────────────────────────────────────────
    // 7. SUMMARY STATS
    // ─────────────────────────────────────────────
    public Map<String, Object> getSummaryStats(int days) {
        LocalDate from = LocalDate.now().minusDays(days);
        List<Bill> bills = billRepository
                .findByBillDateGreaterThanEqual(from);

        BigDecimal totalRevenue = bills.stream()
                .map(b -> b.getTotalAmount() != null
                        ? b.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long uniqueCustomers = bills.stream()
                .filter(b -> b.getCustomer() != null)
                .map(b -> b.getCustomer().getId())
                .distinct().count();

        BigDecimal avgBillValue = bills.isEmpty()
                ? BigDecimal.ZERO
                : totalRevenue.divide(
                BigDecimal.valueOf(bills.size()),
                2, RoundingMode.HALF_UP);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRevenue",    totalRevenue
                .setScale(2, RoundingMode.HALF_UP));
        stats.put("totalBills",      bills.size());
        stats.put("uniqueCustomers", uniqueCustomers);
        stats.put("avgBillValue",    avgBillValue);
        stats.put("days",            days);
        return stats;
    }
}