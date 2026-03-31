package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.entity.BillItem;
import com.example.grocery_billing.entity.Customer;
import com.example.grocery_billing.entity.Product;
import com.example.grocery_billing.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagDataService {

    private final BillRepository        billRepository;
    private final ProductRepository     productRepository;
    private final CustomerRepository    customerRepository;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ─────────────────────────────────────────────
    // Build full context for Claude based on question
    // ─────────────────────────────────────────────
    public String buildContext(String question) {
        StringBuilder ctx = new StringBuilder();
        String q = question.toLowerCase();

        ctx.append("You are a smart business assistant for a grocery shop. ")
                .append("Answer questions based on the data provided below. ")
                .append("Be concise, helpful and use ₹ for currency. ")
                .append("Format numbers clearly. Use bullet points where helpful.\n\n");

        // Sales related
        if (q.contains("sale") || q.contains("revenue")
                || q.contains("bill") || q.contains("today")
                || q.contains("week") || q.contains("month")
                || q.contains("best") || q.contains("top")) {
            ctx.append(getSalesContext());
        }

        // Product / inventory related
        if (q.contains("product") || q.contains("stock")
                || q.contains("item") || q.contains("inventory")
                || q.contains("order") || q.contains("low")
                || q.contains("sell") || q.contains("popular")) {
            ctx.append(getProductContext());
        }

        // Customer related
        if (q.contains("customer") || q.contains("buyer")
                || q.contains("client") || q.contains("credit")
                || q.contains("udhari") || q.contains("loyal")
                || q.contains("visit") || q.contains("who")) {
            ctx.append(getCustomerContext());
        }

        // Always add summary
        ctx.append(getQuickSummary());

        return ctx.toString();
    }

    // ─────────────────────────────────────────────
    // SALES CONTEXT
    // ─────────────────────────────────────────────
    private String getSalesContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== SALES DATA ===\n");

        // Today
        LocalDate today = LocalDate.now();
        List<Bill> todayBills = billRepository
                .findByBillDateGreaterThanEqual(today);
        BigDecimal todayRevenue = todayBills.stream()
                .map(b -> b.getTotalAmount() != null
                        ? b.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        sb.append("Today (").append(today.format(FMT)).append("):\n");
        sb.append("  Bills: ").append(todayBills.size()).append("\n");
        sb.append("  Revenue: ₹").append(
                todayRevenue.setScale(2, RoundingMode.HALF_UP)).append("\n\n");

        // Last 7 days
        LocalDate week = today.minusDays(7);
        List<Bill> weekBills = billRepository
                .findByBillDateGreaterThanEqual(week);
        BigDecimal weekRevenue = weekBills.stream()
                .map(b -> b.getTotalAmount() != null
                        ? b.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        sb.append("Last 7 Days:\n");
        sb.append("  Bills: ").append(weekBills.size()).append("\n");
        sb.append("  Revenue: ₹").append(
                weekRevenue.setScale(2, RoundingMode.HALF_UP)).append("\n\n");

        // Last 30 days
        LocalDate month = today.minusDays(30);
        List<Bill> monthBills = billRepository
                .findByBillDateGreaterThanEqual(month);
        BigDecimal monthRevenue = monthBills.stream()
                .map(b -> b.getTotalAmount() != null
                        ? b.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        sb.append("Last 30 Days:\n");
        sb.append("  Bills: ").append(monthBills.size()).append("\n");
        sb.append("  Revenue: ₹").append(
                monthRevenue.setScale(2, RoundingMode.HALF_UP)).append("\n\n");

        // Top selling products last 30 days
        Map<String, Double> productQty = new HashMap<>();
        Map<String, BigDecimal> productRevenue = new HashMap<>();
        for (Bill bill : monthBills) {
            for (BillItem item : bill.getBillItems()) {
                String name = item.getProductNameSnapshot() != null
                        ? item.getProductNameSnapshot()
                        : item.getProduct().getNameEn();
                productQty.merge(name,
                        item.getQuantity().doubleValue(), Double::sum);
                productRevenue.merge(name,
                        item.getItemTotal(), BigDecimal::add);
            }
        }

        sb.append("Top 5 Products (last 30 days):\n");
        productQty.entrySet().stream()
                .sorted(Map.Entry.<String, Double>
                        comparingByValue().reversed())
                .limit(5)
                .forEach(e -> sb.append("  - ")
                        .append(e.getKey())
                        .append(": qty=").append(e.getValue())
                        .append(", revenue=₹")
                        .append(productRevenue.getOrDefault(
                                        e.getKey(), BigDecimal.ZERO)
                                .setScale(2, RoundingMode.HALF_UP))
                        .append("\n"));

        // Payment methods breakdown
        sb.append("\nPayment Methods (last 30 days):\n");
        Map<String, Long> paymentMethods = monthBills.stream()
                .collect(Collectors.groupingBy(
                        b -> b.getPaymentMethod() != null
                                ? b.getPaymentMethod() : "UNKNOWN",
                        Collectors.counting()));
        paymentMethods.forEach((method, count) ->
                sb.append("  ").append(method)
                        .append(": ").append(count).append(" bills\n"));

        sb.append("\n");
        return sb.toString();
    }

    // ─────────────────────────────────────────────
    // PRODUCT / INVENTORY CONTEXT
    // ─────────────────────────────────────────────
    private String getProductContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== INVENTORY DATA ===\n");

        List<Product> products = productRepository.findAll();
        sb.append("Total Products: ").append(products.size()).append("\n\n");

        // Low stock products
        sb.append("Low Stock Products (qty < 10):\n");
        products.stream()
                .filter(p -> p.getStockQty() != null
                        && p.getStockQty() < 10)
                .forEach(p -> sb.append("  - ")
                        .append(p.getNameEn())
                        .append(": stock=")
                        .append(p.getStockQty())
                        .append(" ").append(p.getUnit() != null
                                ? p.getUnit() : "")
                        .append("\n"));

        // Out of stock
        sb.append("\nOut of Stock:\n");
        products.stream()
                .filter(p -> p.getStockQty() != null
                        && p.getStockQty() <= 0)
                .forEach(p -> sb.append("  - ")
                        .append(p.getNameEn()).append("\n"));

        // All products with stock
        sb.append("\nAll Products Stock:\n");
        products.stream()
                .limit(20)
                .forEach(p -> sb.append("  - ")
                        .append(p.getNameEn())
                        .append(": stock=")
                        .append(p.getStockQty())
                        .append(", price=₹")
                        .append(p.getPrice())
                        .append("\n"));

        sb.append("\n");
        return sb.toString();
    }
    // ─────────────────────────────────────────────
// DETECT LANGUAGE FROM QUESTION
// ─────────────────────────────────────────────
    public String detectLanguage(String text) {
        if (text == null) return "en";
        // Check for Hindi/Marathi Devanagari characters
        boolean hasDevanagari = text.chars().anyMatch(c ->
                Character.UnicodeBlock.of(c) ==
                        Character.UnicodeBlock.DEVANAGARI);
        if (hasDevanagari) {
            // Basic check for Marathi-specific words
            if (text.contains("आहे") || text.contains("काय")
                    || text.contains("किती")
                    || text.contains("कसे")) {
                return "mr"; // Marathi
            }
            return "hi"; // Hindi
        }
        return "en"; // English
    }
    // ─────────────────────────────────────────────
    // CUSTOMER CONTEXT
    // ─────────────────────────────────────────────
    private String getCustomerContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CUSTOMER DATA ===\n");

        List<Customer> customers = customerRepository.findAll();
        sb.append("Total Customers: ").append(customers.size()).append("\n\n");

        // Top customers by spending
        sb.append("Top Customers by Total Paid:\n");
        customers.stream()
                .filter(c -> c.getTotalPaid() != null)
                .sorted(Comparator.comparing(
                        Customer::getTotalPaid).reversed())
                .limit(10)
                .forEach(c -> sb.append("  - ")
                        .append(c.getName())
                        .append(": paid=₹").append(c.getTotalPaid())
                        .append(", balance=₹").append(c.getBalance())
                        .append("\n"));

        // Customers with credit (udhari)
        sb.append("\nCustomers with Pending Udhari:\n");
        customers.stream()
                .filter(c -> c.getBalance() != null
                        && c.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(
                        Customer::getBalance).reversed())
                .limit(10)
                .forEach(c -> sb.append("  - ")
                        .append(c.getName())
                        .append(": pending=₹").append(c.getBalance())
                        .append(", phone=").append(c.getPhone())
                        .append("\n"));

        // Recent customers
        sb.append("\nRecent Bills with Customers (last 7 days):\n");
        LocalDate week = LocalDate.now().minusDays(7);
        billRepository.findByBillDateGreaterThanEqual(week)
                .stream()
                .filter(b -> b.getCustomer() != null && b.getBillDate() != null)
                .forEach(b -> sb.append("  - ")
                        .append(b.getCustomer().getName())
                        .append(" on ").append(b.getBillDate().format(FMT))
                        .append(" ₹").append(b.getTotalAmount() != null
                                ? b.getTotalAmount() : "0")
                        .append("\n"));

        sb.append("\n");
        return sb.toString();
    }

    // ─────────────────────────────────────────────
    // QUICK SUMMARY
    // ─────────────────────────────────────────────
    private String getQuickSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== SHOP SUMMARY ===\n");

        long totalBills = billRepository.count();
        long totalProducts = productRepository.count();
        long totalCustomers = customerRepository.count();

        sb.append("Total Bills Ever: ").append(totalBills).append("\n");
        sb.append("Total Products: ").append(totalProducts).append("\n");
        sb.append("Total Customers: ").append(totalCustomers).append("\n");

        sb.append("\n");
        return sb.toString();
    }
}