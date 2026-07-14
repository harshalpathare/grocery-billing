package com.example.grocery_billing.service;

import com.example.grocery_billing.dto.JournalEntryDto;
import com.example.grocery_billing.entity.*;
import com.example.grocery_billing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountingService {

    private final BillRepository billRepository;
    private final ExpenseRepository expenseRepository;
    private final TransactionRepository transactionRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final ShopService shopService;

    /**
     * Generates all Journal Entries dynamically for a specific shop and date range.
     */
    public List<JournalEntryDto> generateJournal(LocalDate start, LocalDate end) {
        Shop shop = shopService.requireCurrentShop();
        List<JournalEntryDto> entries = new ArrayList<>();

        // 1. BILLS (Sales)
        List<Bill> bills = billRepository.findByShopIdAndBillDateBetweenOrderByBillDateDesc(shop.getId(), start, end);
        for (Bill bill : bills) {
            String ref = bill.getBillNo();
            String narration = "Sales to " + (bill.getCustomer() != null ? bill.getCustomer().getName() : (bill.getWalkInCustomerName() != null && !bill.getWalkInCustomerName().isBlank() ? bill.getWalkInCustomerName() : "Cash Customer"));
            
            if ("PAID".equals(bill.getPaymentStatus().name())) {
                String account = "UPI".equals(bill.getPaymentMethod()) ? "Bank" : "Cash";
                entries.add(createEntry(bill.getCreatedAt(), ref, account, bill.getTotalAmount(), null, narration));
                entries.add(createEntry(bill.getCreatedAt(), ref, "Sales Account", null, bill.getTotalAmount(), narration));
            } else if ("CREDIT".equals(bill.getPaymentStatus().name())) {
                String acc = "Customer - " + (bill.getCustomer() != null ? bill.getCustomer().getName() : "Unknown");
                entries.add(createEntry(bill.getCreatedAt(), ref, acc, bill.getTotalAmount(), null, narration));
                entries.add(createEntry(bill.getCreatedAt(), ref, "Sales Account", null, bill.getTotalAmount(), narration));
            } else if ("PARTIAL".equals(bill.getPaymentStatus().name())) {
                String account = "UPI".equals(bill.getPaymentMethod()) ? "Bank" : "Cash";
                
                // Fetch the actual paid amount from Transaction repository since Bill doesn't store paid amount directly
                List<Transaction> billTxs = transactionRepository.findByBillId(bill.getId());
                BigDecimal paid = billTxs.stream()
                        .filter(t -> "DEBIT".equals(t.getType().name()))
                        .map(Transaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal credit = bill.getTotalAmount().subtract(paid);
                String acc = "Customer - " + (bill.getCustomer() != null ? bill.getCustomer().getName() : "Unknown");
                
                if (paid.compareTo(BigDecimal.ZERO) > 0) {
                    entries.add(createEntry(bill.getCreatedAt(), ref, account, paid, null, narration + " (Paid)"));
                }
                if (credit.compareTo(BigDecimal.ZERO) > 0) {
                    entries.add(createEntry(bill.getCreatedAt(), ref, acc, credit, null, narration + " (Credit)"));
                }
                entries.add(createEntry(bill.getCreatedAt(), ref, "Sales Account", null, bill.getTotalAmount(), narration));
            }
        }

        // 2. EXPENSES
        List<Expense> expenses = expenseRepository.findByShopIdAndExpenseDateBetweenOrderByExpenseDateDesc(shop.getId(), start, end);
        for (Expense exp : expenses) {
            String ref = "EXP-" + exp.getId();
            String narration = exp.getNote() != null ? exp.getNote() : "Expense: " + exp.getCategory();
            // Default to Cash for expenses if no payment mode is explicitly tracked in Expense
            entries.add(createEntry(exp.getCreatedAt(), ref, "Expense - " + exp.getCategory(), exp.getAmount(), null, narration));
            entries.add(createEntry(exp.getCreatedAt(), ref, "Cash", null, exp.getAmount(), narration));
        }

        // 3. CUSTOMER PAYMENTS (Udhari Payments)
        List<Transaction> transactions = transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(start, end);
        for (Transaction trx : transactions) {
            if (trx.getCustomer() != null && trx.getCustomer().getShop().getId().equals(shop.getId())) {
                if ("DEBIT".equals(trx.getType().name())) {
                    // Customer paid us
                    String ref = "TRX-" + trx.getId();
                    String narration = trx.getDescription() != null ? trx.getDescription() : "Received from customer";
                    entries.add(createEntry(trx.getCreatedAt(), ref, "Cash", trx.getAmount(), null, narration));
                    entries.add(createEntry(trx.getCreatedAt(), ref, "Customer - " + trx.getCustomer().getName(), null, trx.getAmount(), narration));
                }
            }
        }

        // 4. PURCHASES (Purchase Orders)
        List<PurchaseOrder> pos = purchaseOrderRepository.findByOrderDateBetweenOrderByOrderDateDesc(start, end);
        for (PurchaseOrder po : pos) {
            if (po.getSupplier() != null && po.getSupplier().getShop().getId().equals(shop.getId())) {
                String ref = po.getPoNumber();
                String narration = "Purchase from " + po.getSupplier().getName();
                
                BigDecimal total = po.getTotalAmount();
                BigDecimal paid = po.getAmountPaid() != null ? po.getAmountPaid() : BigDecimal.ZERO;
                BigDecimal credit = total.subtract(paid);
                
                entries.add(createEntry(po.getCreatedAt(), ref, "Purchase Account", total, null, narration));
                
                if (paid.compareTo(BigDecimal.ZERO) > 0) {
                    entries.add(createEntry(po.getCreatedAt(), ref, "Cash", null, paid, narration + " (Paid)"));
                }
                if (credit.compareTo(BigDecimal.ZERO) > 0) {
                    entries.add(createEntry(po.getCreatedAt(), ref, "Supplier - " + po.getSupplier().getName(), null, credit, narration + " (Credit)"));
                }
            }
        }

        // 5. SUPPLIER PAYMENTS
        // The repository doesn't have a date filter right now, so fetch all and filter in memory, or we can just fetch all for now
        List<SupplierPayment> sps = supplierPaymentRepository.findAll();
        for (SupplierPayment sp : sps) {
            if (sp.getSupplier() != null && sp.getSupplier().getShop().getId().equals(shop.getId())) {
                if (!sp.getPaymentDate().isBefore(start) && !sp.getPaymentDate().isAfter(end)) {
                    String ref = sp.getReferenceNo() != null && !sp.getReferenceNo().isBlank() ? sp.getReferenceNo() : "SP-" + sp.getId();
                    String narration = sp.getNotes() != null ? sp.getNotes() : "Payment to supplier";
                    String acc = "UPI".equalsIgnoreCase(sp.getPaymentMode()) || "Bank".equalsIgnoreCase(sp.getPaymentMode()) ? "Bank" : "Cash";
                    
                    entries.add(createEntry(sp.getCreatedAt(), ref, "Supplier - " + sp.getSupplier().getName(), sp.getAmount(), null, narration));
                    entries.add(createEntry(sp.getCreatedAt(), ref, acc, null, sp.getAmount(), narration));
                }
            }
        }

        // Sort by Date Ascending
        entries.sort(Comparator.comparing(JournalEntryDto::getDate, Comparator.nullsLast(Comparator.naturalOrder())));
        return entries;
    }

    /**
     * Generates the Ledger for a specific account.
     */
    public List<JournalEntryDto> getLedger(String accountName, LocalDate start, LocalDate end) {
        List<JournalEntryDto> list = generateJournal(start, end).stream()
                .filter(e -> e.getAccount().equalsIgnoreCase(accountName))
                .collect(Collectors.toList());
        
        BigDecimal balance = BigDecimal.ZERO;
        for (JournalEntryDto e : list) {
            BigDecimal dr = e.getDebit() != null ? e.getDebit() : BigDecimal.ZERO;
            BigDecimal cr = e.getCredit() != null ? e.getCredit() : BigDecimal.ZERO;
            balance = balance.add(dr).subtract(cr);
            e.setRunningBalance(balance);
        }
        return list;
    }

    /**
     * Extracts unique account names from the journal.
     */
    public List<String> getAllAccountNames(LocalDate start, LocalDate end) {
        return generateJournal(start, end).stream()
                .map(JournalEntryDto::getAccount)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Generates a Trial Balance
     */
    public Map<String, BigDecimal> getTrialBalance(LocalDate start, LocalDate end) {
        List<JournalEntryDto> entries = generateJournal(start, end);
        Map<String, BigDecimal> tb = new TreeMap<>();
        
        for (JournalEntryDto e : entries) {
            BigDecimal dr = e.getDebit() != null ? e.getDebit() : BigDecimal.ZERO;
            BigDecimal cr = e.getCredit() != null ? e.getCredit() : BigDecimal.ZERO;
            BigDecimal net = dr.subtract(cr);
            
            tb.put(e.getAccount(), tb.getOrDefault(e.getAccount(), BigDecimal.ZERO).add(net));
        }
        return tb;
    }

    private JournalEntryDto createEntry(java.time.LocalDateTime date, String ref, String account, BigDecimal dr, BigDecimal cr, String narration) {
        if (date == null) date = java.time.LocalDateTime.now();
        return JournalEntryDto.builder()
                .date(date)
                .reference(ref)
                .account(account)
                .debit(dr)
                .credit(cr)
                .narration(narration)
                .build();
    }
}
