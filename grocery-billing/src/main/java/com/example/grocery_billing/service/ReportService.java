package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.entity.Customer;
import com.example.grocery_billing.entity.Transaction;
import com.example.grocery_billing.repository.BillRepository;
import com.example.grocery_billing.repository.CustomerRepository;
import com.example.grocery_billing.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

/**
 * REPORT SERVICE
 *
 * All reporting queries live here.
 * Controllers call this service and pass results to HTML templates.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final BillRepository        billRepository;
    private final CustomerRepository    customerRepository;
    private final TransactionRepository transactionRepository;

    // ─────────────────────────────────────────────────────
    // DAILY REPORT
    // Returns all bills for a given date
    // ─────────────────────────────────────────────────────
    public DailyReport getDailyReport(LocalDate date) {
        List<Bill> bills = billRepository.findByBillDateOrderByCreatedAtDesc(date);

        BigDecimal totalSales  = bills.stream()
                .map(Bill::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGst    = bills.stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsGst()))
                .map(b -> b.getGstAmount() != null ? b.getGstAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long paidCount   = bills.stream()
                .filter(b -> Bill.PaymentStatus.PAID.equals(b.getPaymentStatus()))
                .count();
        long creditCount = bills.stream()
                .filter(b -> Bill.PaymentStatus.CREDIT.equals(b.getPaymentStatus()))
                .count();

        return new DailyReport(date, bills, totalSales, totalGst,
                bills.size(), paidCount, creditCount);
    }

    // ─────────────────────────────────────────────────────
    // MONTHLY REPORT
    // Returns month-by-month summary for a given year
    // ─────────────────────────────────────────────────────
    public List<MonthlyRow> getMonthlyReport(int year) {
        List<MonthlyRow> rows = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            BigDecimal sales = billRepository.getTotalSalesByMonth(year, month);
            BigDecimal gst   = getMonthlyGst(year, month);
            long count       = countBillsByMonth(year, month);
            String monthName = Month.of(month)
                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            rows.add(new MonthlyRow(monthName, month, sales, gst, count));
        }

        return rows;
    }

    private BigDecimal getMonthlyGst(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());
        return billRepository.findByIsGstTrueAndBillDateBetween(start, end)
                .stream()
                .map(b -> b.getGstAmount() != null ? b.getGstAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countBillsByMonth(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.withDayOfMonth(start.lengthOfMonth());
        return billRepository.findByBillDateBetweenOrderByBillDateDesc(start, end).size();
    }

    // ─────────────────────────────────────────────────────
    // GST REPORT
    // Returns all GST bills in a date range with CGST/SGST breakdown
    // ─────────────────────────────────────────────────────
    public GstReport getGstReport(LocalDate start, LocalDate end) {
        List<Bill> gstBills = billRepository
                .findByIsGstTrueAndBillDateBetween(start, end);

        BigDecimal totalTaxable = gstBills.stream()
                .map(b -> b.getSubtotal() != null ? b.getSubtotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalGst = gstBills.stream()
                .map(b -> b.getGstAmount() != null ? b.getGstAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cgst = totalGst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal sgst = cgst;

        return new GstReport(start, end, gstBills, totalTaxable,
                totalGst, cgst, sgst);
    }

    // ─────────────────────────────────────────────────────
    // CREDIT / DUES REPORT
    // Returns customers with pending balance + all transactions
    // ─────────────────────────────────────────────────────
    public CreditReport getCreditReport() {
        List<Customer> customersWithDues =
                customerRepository.findByBalanceGreaterThanAndActiveTrue(BigDecimal.ZERO);

        BigDecimal totalDues = customerRepository.getTotalPendingBalance();

        // Recent credit transactions (last 30 days)
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<Transaction> recentTransactions =
                transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(
                        thirtyDaysAgo, LocalDate.now());

        return new CreditReport(customersWithDues, totalDues, recentTransactions);
    }

    // ─────────────────────────────────────────────────────
    // SUMMARY STATS (for dashboard top cards)
    // ─────────────────────────────────────────────────────
    public SummaryStats getSummaryStats() {
        LocalDate today     = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        BigDecimal todaySales   = billRepository.getTotalSalesByDate(today);
        BigDecimal monthSales   = billRepository.getTotalSalesByMonth(
                today.getYear(), today.getMonthValue());
        BigDecimal totalPending = customerRepository.getTotalPendingBalance();
        long todayBills         = billRepository.countByBillDate(today);

        // This month GST
        BigDecimal monthGst = getMonthlyGst(today.getYear(), today.getMonthValue());

        return new SummaryStats(todaySales, monthSales, totalPending,
                todayBills, monthGst);
    }

    // ─────────────────────────────────────────────────────
    // CSV DATA — bills in date range
    // ─────────────────────────────────────────────────────
    public List<Bill> getBillsForExport(LocalDate start, LocalDate end) {
        return billRepository.findByBillDateBetweenOrderByBillDateDesc(start, end);
    }

    // ─────────────────────────────────────────────────────
    // RECORD CLASSES (simple data containers)
    // These carry data from service → controller → HTML
    // ─────────────────────────────────────────────────────

    public record DailyReport(
            LocalDate date,
            List<Bill> bills,
            BigDecimal totalSales,
            BigDecimal totalGst,
            int billCount,
            long paidCount,
            long creditCount
    ) {}

    public record MonthlyRow(
            String monthName,
            int monthNumber,
            BigDecimal sales,
            BigDecimal gst,
            long billCount
    ) {}

    public record GstReport(
            LocalDate startDate,
            LocalDate endDate,
            List<Bill> bills,
            BigDecimal totalTaxable,
            BigDecimal totalGst,
            BigDecimal cgst,
            BigDecimal sgst
    ) {}

    public record CreditReport(
            List<Customer> customersWithDues,
            BigDecimal totalDues,
            List<Transaction> recentTransactions
    ) {}

    public record SummaryStats(
            BigDecimal todaySales,
            BigDecimal monthSales,
            BigDecimal totalPending,
            long todayBills,
            BigDecimal monthGst
    ) {}
}