package com.example.grocery_billing.service;

import com.example.grocery_billing.config.ShopContext;
import com.example.grocery_billing.entity.Shop;
import com.example.grocery_billing.entity.Supplier;
import com.example.grocery_billing.repository.ShopRepository;
import com.example.grocery_billing.repository.SupplierRepository;
import com.example.grocery_billing.repository.CashFlowRepository;
import com.example.grocery_billing.repository.SupplierPaymentRepository;
import com.example.grocery_billing.repository.PurchaseOrderRepository;
import com.example.grocery_billing.entity.CashFlow;
import com.example.grocery_billing.entity.SupplierPayment;
import com.example.grocery_billing.entity.PurchaseOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ShopRepository     shopRepository;
    private final CashFlowRepository cashFlowRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    // ── helpers ───────────────────────────────────────────
    private Long shopId() { return ShopContext.getShopId(); }

    private Shop currentShop() {
        Long id = shopId();
        if (id == null) throw new RuntimeException("No shop in context");
        return shopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop not found: " + id));
    }

    // ─────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────

    public List<Supplier> getAllActive() {
        Long id = shopId();
        if (id != null) return supplierRepository.findByShopIdAndActiveTrueOrderByNameAsc(id);
        return supplierRepository.findByActiveTrueOrderByNameAsc();
    }

    public Supplier getById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found: " + id));
    }

    public BigDecimal getTotalPayable() {
        Long id = shopId();
        if (id != null) return supplierRepository.getTotalPayableByShop(id);
        return supplierRepository.getTotalPayable();
    }

    public List<Supplier> getSuppliersWithPendingBalance() {
        Long id = shopId();
        if (id != null) return supplierRepository.findSuppliersWithPendingBalance(id);
        return supplierRepository.findAllSuppliersWithPendingBalance();
    }

    // ─────────────────────────────────────────────────────
    // WRITE
    // ─────────────────────────────────────────────────────

    @Transactional
    public Supplier save(Supplier supplier) {
        if (supplier.getShop() == null) supplier.setShop(currentShop());
        if (supplier.getName()  != null) supplier.setName(supplier.getName().trim());
        if (supplier.getPhone() != null) supplier.setPhone(supplier.getPhone().trim());
        return supplierRepository.save(supplier);
    }

    @Transactional
    public void delete(Long id) {
        Supplier s = getById(id);
        s.setActive(false);
        supplierRepository.save(s);
    }

    @Transactional
    public void recordPayment(Long supplierId, BigDecimal amount) {
        Supplier s = getById(supplierId);
        BigDecimal currentPaid = s.getTotalPaid()    != null ? s.getTotalPaid()    : BigDecimal.ZERO;
        BigDecimal payable     = s.getTotalPayable() != null ? s.getTotalPayable() : BigDecimal.ZERO;
        BigDecimal maxPayable  = payable.subtract(currentPaid);
        BigDecimal safeAmount  = amount.min(maxPayable);
        if (safeAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("No pending balance to pay.");
        s.setTotalPaid(currentPaid.add(safeAmount));
        s.updateBalance();
        supplierRepository.saveAndFlush(s);

        // ✅ Save Supplier Payment History
        SupplierPayment payment = new SupplierPayment();
        payment.setSupplier(s);
        payment.setAmount(safeAmount);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentMode("Cash");
        payment.setNotes("Payment recorded from dashboard");
        supplierPaymentRepository.save(payment);

        // ✅ Automatic Cash Flow (OUT)
        CashFlow cf = new CashFlow();
        cf.setShop(s.getShop());
        cf.setTransactionDate(LocalDate.now());
        cf.setType(CashFlow.TransactionType.OUT);
        cf.setAmount(safeAmount);
        cf.setCategory("Supplier Payment");
        cf.setDescription("Payment to: " + s.getName());
        cashFlowRepository.save(cf);
    }

    public record LedgerEntry(LocalDate date, String particulars, String type, BigDecimal amount, BigDecimal balance) {}

    public List<LedgerEntry> getLedger(Long supplierId) {
        Supplier supplier = getById(supplierId);
        List<PurchaseOrder> purchases = purchaseOrderRepository.findAll().stream()
            .filter(p -> p.getSupplier() != null && p.getSupplier().getId().equals(supplierId))
            .toList(); // Should ideally use a repository method for efficiency, but this is simple enough for now
        List<SupplierPayment> payments = supplierPaymentRepository.findBySupplierIdOrderByPaymentDateDescCreatedAtDesc(supplierId);

        List<LedgerEntry> combined = new ArrayList<>();
        
        for (PurchaseOrder p : purchases) {
            combined.add(new LedgerEntry(p.getOrderDate(), "Purchase Invoice #" + p.getPoNumber(), "CREDIT", p.getTotalAmount(), BigDecimal.ZERO));
        }
        for (SupplierPayment sp : payments) {
            combined.add(new LedgerEntry(sp.getPaymentDate(), "Payment Received - " + sp.getPaymentMode(), "DEBIT", sp.getAmount(), BigDecimal.ZERO));
        }

        combined.sort(Comparator.comparing(LedgerEntry::date));

        List<LedgerEntry> finalLedger = new ArrayList<>();
        BigDecimal runningBalance = BigDecimal.ZERO;
        
        for (LedgerEntry entry : combined) {
            if ("CREDIT".equals(entry.type())) {
                runningBalance = runningBalance.add(entry.amount());
            } else {
                runningBalance = runningBalance.subtract(entry.amount());
            }
            finalLedger.add(new LedgerEntry(entry.date(), entry.particulars(), entry.type(), entry.amount(), runningBalance));
        }
        
        // Reverse so latest is on top, just like a bank statement
        java.util.Collections.reverse(finalLedger);
        return finalLedger;
    }
}
