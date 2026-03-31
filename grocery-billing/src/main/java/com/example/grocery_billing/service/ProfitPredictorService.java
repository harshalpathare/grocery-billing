package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.entity.BillItem;
import com.example.grocery_billing.entity.PurchaseOrder;
import com.example.grocery_billing.entity.PurchaseOrderItem;
import com.example.grocery_billing.repository.BillRepository;
import com.example.grocery_billing.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfitPredictorService {

    private final BillRepository          billRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    private static final DateTimeFormatter MONTH_FMT =
            DateTimeFormatter.ofPattern("MMM yyyy");

    // ─────────────────────────────────────────────
    // 1. MONTHLY PROFIT ANALYSIS
    // Revenue - Purchase Cost = Profit
    // ─────────────────────────────────────────────
    public List<Map<String, Object>> getMonthlyProfitAnalysis(
            int months) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = LocalDate.now()
                    .minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd   = monthStart
                    .plusMonths(1).minusDays(1);

            // Revenue from bills
            BigDecimal revenue = billRepository.findAll()
                    .stream()
                    .filter(b -> !b.getBillDate()
                            .isBefore(monthStart)
                            && !b.getBillDate()
                            .isAfter(monthEnd))
                    .map(b -> b.getTotalAmount() != null
                            ? b.getTotalAmount()
                            : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Purchase cost
            BigDecimal purchaseCost = purchaseOrderRepository
                    .findAll().stream()
                    .filter(po -> po.getOrderDate() != null
                            && !po.getOrderDate()
                            .isBefore(monthStart)
                            && !po.getOrderDate()
                            .isAfter(monthEnd))
                    .map(po -> po.getTotalAmount() != null
                            ? po.getTotalAmount()
                            : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal profit = revenue.subtract(purchaseCost);
            BigDecimal margin = revenue.compareTo(
                    BigDecimal.ZERO) > 0
                    ? profit.divide(revenue, 4,
                            RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month",         monthStart.format(MONTH_FMT));
            m.put("monthKey",
                    monthStart.getYear() + "-"
                            + String.format("%02d",
                            monthStart.getMonthValue()));
            m.put("revenue",
                    revenue.setScale(2, RoundingMode.HALF_UP));
            m.put("purchaseCost",
                    purchaseCost.setScale(2, RoundingMode.HALF_UP));
            m.put("profit",
                    profit.setScale(2, RoundingMode.HALF_UP));
            m.put("margin",        margin);
            result.add(m);
        }

        return result;
    }

    // ─────────────────────────────────────────────
    // 2. PREDICT NEXT MONTH REVENUE
    // Uses weighted average of last 3 months
    // ─────────────────────────────────────────────
    public Map<String, Object> predictNextMonth() {

        List<Map<String, Object>> history =
                getMonthlyProfitAnalysis(6);

        // Weighted average — recent months get more weight
        double[] weights = {0.1, 0.15, 0.2, 0.25, 0.3};
        double weightedRevenue  = 0;
        double weightedProfit   = 0;
        double totalWeight      = 0;

        int start = Math.max(0, history.size() - 5);
        int wi    = 0;
        for (int i = start; i < history.size(); i++) {
            double w = weights[Math.min(wi,
                    weights.length - 1)];
            double rev = ((BigDecimal) history.get(i)
                    .get("revenue")).doubleValue();
            double pft = ((BigDecimal) history.get(i)
                    .get("profit")).doubleValue();
            weightedRevenue += rev * w;
            weightedProfit  += pft * w;
            totalWeight     += w;
            wi++;
        }

        double predictedRevenue = totalWeight > 0
                ? weightedRevenue / totalWeight : 0;
        double predictedProfit  = totalWeight > 0
                ? weightedProfit / totalWeight : 0;

        // Growth trend
        String trend = "stable";
        if (history.size() >= 2) {
            double lastMonth =
                    ((BigDecimal) history.get(
                                    history.size() - 1)
                            .get("revenue")).doubleValue();
            double prevMonth =
                    ((BigDecimal) history.get(
                                    history.size() - 2)
                            .get("revenue")).doubleValue();
            if (lastMonth > prevMonth * 1.05)
                trend = "growing 📈";
            else if (lastMonth < prevMonth * 0.95)
                trend = "declining 📉";
            else
                trend = "stable ➡️";
        }

        LocalDate nextMonth = LocalDate.now().plusMonths(1);

        Map<String, Object> prediction = new LinkedHashMap<>();
        prediction.put("month",
                nextMonth.format(MONTH_FMT));
        prediction.put("predictedRevenue",
                BigDecimal.valueOf(predictedRevenue)
                        .setScale(2, RoundingMode.HALF_UP));
        prediction.put("predictedProfit",
                BigDecimal.valueOf(predictedProfit)
                        .setScale(2, RoundingMode.HALF_UP));
        prediction.put("trend", trend);
        prediction.put("confidence",
                history.size() >= 3 ? "High" : "Low");

        return prediction;
    }

    // ─────────────────────────────────────────────
    // 3. TOP PROFIT PRODUCTS
    // ─────────────────────────────────────────────
    public List<Map<String, Object>> getTopProfitProducts(
            int days) {

        LocalDate from = LocalDate.now().minusDays(days);
        Map<String, BigDecimal> productRevenue = new HashMap<>();
        Map<String, BigDecimal> productCost    = new HashMap<>();

        // Revenue side
        billRepository.findByBillDateGreaterThanEqual(from)
                .forEach(bill -> {
                    for (BillItem item : bill.getBillItems()) {
                        String name =
                                item.getProductNameSnapshot()
                                        != null
                                        ? item.getProductNameSnapshot()
                                        : item.getProduct().getNameEn();
                        productRevenue.merge(name,
                                item.getItemTotal(),
                                BigDecimal::add);

                        // Estimate cost as 70% of selling price
                        BigDecimal estCost = item.getItemTotal()
                                .multiply(
                                        BigDecimal.valueOf(0.7));
                        productCost.merge(name,
                                estCost, BigDecimal::add);
                    }
                });

        return productRevenue.entrySet().stream()
                .map(e -> {
                    BigDecimal rev  = e.getValue();
                    BigDecimal cost = productCost
                            .getOrDefault(e.getKey(),
                                    BigDecimal.ZERO);
                    BigDecimal profit = rev.subtract(cost);
                    BigDecimal margin = rev.compareTo(
                            BigDecimal.ZERO) > 0
                            ? profit.divide(rev, 4,
                                    RoundingMode.HALF_UP)
                            .multiply(
                                    BigDecimal.valueOf(100))
                            .setScale(1,
                                    RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    Map<String, Object> m =
                            new LinkedHashMap<>();
                    m.put("product",  e.getKey());
                    m.put("revenue",
                            rev.setScale(2,
                                    RoundingMode.HALF_UP));
                    m.put("estimatedProfit",
                            profit.setScale(2,
                                    RoundingMode.HALF_UP));
                    m.put("margin",   margin);
                    return m;
                })
                .sorted((a, b) -> ((BigDecimal)
                        b.get("estimatedProfit"))
                        .compareTo((BigDecimal)
                                a.get("estimatedProfit")))
                .limit(10)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // 4. OVERALL SUMMARY
    // ─────────────────────────────────────────────
    public Map<String, Object> getOverallSummary() {
        List<Map<String, Object>> monthly =
                getMonthlyProfitAnalysis(1);

        BigDecimal thisMonthRevenue  = BigDecimal.ZERO;
        BigDecimal thisMonthProfit   = BigDecimal.ZERO;
        BigDecimal thisMonthPurchase = BigDecimal.ZERO;

        if (!monthly.isEmpty()) {
            Map<String, Object> m = monthly.get(0);
            thisMonthRevenue  =
                    (BigDecimal) m.get("revenue");
            thisMonthProfit   =
                    (BigDecimal) m.get("profit");
            thisMonthPurchase =
                    (BigDecimal) m.get("purchaseCost");
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("thisMonthRevenue",  thisMonthRevenue);
        summary.put("thisMonthProfit",   thisMonthProfit);
        summary.put("thisMonthPurchase", thisMonthPurchase);
        summary.put("prediction",        predictNextMonth());
        return summary;
    }
}