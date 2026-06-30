package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.*;
import com.example.grocery_billing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillService {

    private final BillRepository        billRepository;
    private final BillItemRepository    billItemRepository;
    private final ProductRepository     productRepository;
    private final CustomerRepository    customerRepository;
    private final TransactionRepository transactionRepository;

    // ─────────────────────────────────────────────────────
    // READ OPERATIONS
    // ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Bill> getAllBills() {
        return billRepository.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC,
                        "billDate", "id"));
    }

    @Transactional(readOnly = true)
    public Bill getBillById(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Bill> getBillsByDate(LocalDate date) {
        return billRepository.findByBillDateOrderByCreatedAtDesc(date);
    }

    @Transactional(readOnly = true)
    public List<Bill> getBillsBetween(LocalDate start, LocalDate end) {
        return billRepository.findByBillDateBetweenOrderByBillDateDesc(start, end);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTodaySales() {
        return billRepository.getTotalSalesByDate(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public long getTodayBillCount() {
        return billRepository.countByBillDate(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public BigDecimal getMonthlySales(int year, int month) {
        return billRepository.getTotalSalesByMonth(year, month);
    }

    // ─────────────────────────────────────────────────────
    // AUTO GENERATE BILL NUMBER
    // ─────────────────────────────────────────────────────
    public String generateBillNumber() {
        String year   = String.valueOf(LocalDate.now().getYear());
        int    serial = 1;

        Optional<String> lastBillNo = billRepository.findLastBillNoForYear(year);
        if (lastBillNo.isPresent()) {
            try {
                String[] parts = lastBillNo.get().split("-");
                serial = Integer.parseInt(parts[parts.length - 1]) + 1;
            } catch (Exception e) {
                serial = 1;
            }
        }

        String candidate;
        do {
            candidate = String.format("BILL-%s-%04d", year, serial);
            serial++;
        } while (billRepository.existsByBillNo(candidate));

        return candidate;
    }

    // ─────────────────────────────────────────────────────
    // CREATE BILL — main method
    // ─────────────────────────────────────────────────────
    @Transactional
    public Bill createBill(Bill bill,
                           List<BillItemRequest> itemRequests,
                           BigDecimal paidAmount,
                           BigDecimal creditAmount) {

        // Step 1: Bill number + date
        if (bill.getBillNo() == null || bill.getBillNo().trim().isEmpty()) {
            bill.setBillNo(generateBillNumber());
        }
        if (bill.getBillDate() == null) {
            bill.setBillDate(LocalDate.now());
        }

        // Step 2: Extract customer ID BEFORE any lazy loading
        Long customerId = null;
        if (bill.getCustomer() != null) {
            customerId = bill.getCustomer().getId();
        }

        // Step 3: Process items
        for (BillItemRequest req : itemRequests) {
            Product product = productRepository.findById(req.productId())
                    .orElseThrow(() -> new RuntimeException(
                            "Product not found: " + req.productId()));

                BigDecimal displayQuantity = req.quantity() != null
                    ? req.quantity() : BigDecimal.ZERO;
                BigDecimal billedQuantity = displayQuantity;
            String billingUnit = req.unit() != null && !req.unit().isBlank()
                ? req.unit().trim().toLowerCase() : "piece";

            if ("g".equals(billingUnit)
                || "gram".equals(billingUnit)
                || "grams".equals(billingUnit)) {
                billedQuantity = displayQuantity
                .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
            }

            if ("ml".equals(billingUnit)
                || "millilitre".equals(billingUnit)
                || "milliliter".equals(billingUnit)) {
                billedQuantity = displayQuantity
                .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
            }

            BillItem item = new BillItem();
            item.setProduct(product);
            item.setProductNameSnapshot(
                    product.getNameByLanguage(
                            req.lang() != null ? req.lang() : "en"));
                item.setQuantity(billedQuantity);
                item.setDisplayQuantity(displayQuantity);
            item.setUnitPrice(req.unitPrice());
            item.setGstPercent(product.getGstPercent());
            item.setBillingUnit(billingUnit);
                item.setStockQuantity(billedQuantity);
            item.calculateItemTotal();
            bill.addBillItem(item);

            // Reduce stock
            if (product.getStockQty() != null
                    && product.getStockQty().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentStock = product.getStockQty();
                BigDecimal newStock = currentStock.subtract(billedQuantity);
                product.setStockQty(newStock.max(BigDecimal.ZERO));
                productRepository.save(product);
            }
        }

        // Step 4: Calculate totals
        bill.calculateTotals();

        // Step 5: Save bill
        Bill savedBill = billRepository.save(bill);
        log.info("Bill saved: {} | Status: {} | Amount: {} | CustomerId: {}",
                savedBill.getBillNo(),
                savedBill.getPaymentStatus(),
                savedBill.getTotalAmount(),
                customerId);

        // Step 6: Update customer balance
        if (customerId != null) {
            updateCustomerCredit(savedBill, customerId, paidAmount, creditAmount);
        }
        bill.setCreatedAt(LocalDateTime.now());
        return savedBill;
    }
    public List<Bill> getRecentBills(int limit) {
        return billRepository
                .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .limit(limit)
                .toList();
    }
    // ─────────────────────────────────────────────────────
    // UPDATE CUSTOMER CREDIT — extracted as separate method
    // This keeps createBill() clean and avoids the txn bug
    // ─────────────────────────────────────────────────────
    private void updateCustomerCredit(Bill savedBill,
                                      Long customerId,
                                      BigDecimal paidAmount,
                                      BigDecimal creditAmount) {

        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            log.error("Customer not found for id: {}", customerId);
            return;
        }

        // Initialize nulls safely
        if (customer.getTotalCredit() == null)
            customer.setTotalCredit(BigDecimal.ZERO);
        if (customer.getTotalPaid() == null)
            customer.setTotalPaid(BigDecimal.ZERO);

        // Always use TOTAL AMOUNT — not subtotal, not GST amount
        BigDecimal billTotal = savedBill.getTotalAmount() != null
                ? savedBill.getTotalAmount() : BigDecimal.ZERO;

        // ── FULL CREDIT bill ──────────────────────────────
        if (Bill.PaymentStatus.CREDIT.equals(savedBill.getPaymentStatus())) {

            customer.setTotalCredit(
                    customer.getTotalCredit().add(billTotal));
            customer.setBalance(
                    customer.getTotalCredit()
                            .subtract(customer.getTotalPaid()));
            customerRepository.save(customer);

            log.info("Customer {} — credit added: ₹{} | new balance: ₹{}",
                    customer.getName(), billTotal, customer.getBalance());

            // Record CREDIT transaction
            Transaction creditTxn = Transaction.builder()
                    .customer(customer)
                    .bill(savedBill)
                    .type(Transaction.TransactionType.CREDIT)
                    .amount(billTotal)
                    .transactionDate(LocalDate.now())
                    .description("Credit bill: " + savedBill.getBillNo())
                    .balanceAfter(customer.getBalance())
                    .build();
            transactionRepository.save(creditTxn);
        }

        // ── PARTIAL payment bill ──────────────────────────
        else if (Bill.PaymentStatus.PARTIAL.equals(savedBill.getPaymentStatus())) {

            BigDecimal paid     = paidAmount  != null ? paidAmount  : BigDecimal.ZERO;
            BigDecimal onCredit = creditAmount != null ? creditAmount
                    : billTotal.subtract(paid);

            // Only add the CREDIT PORTION to udhari — not the paid portion
            if (onCredit.compareTo(BigDecimal.ZERO) > 0) {
                customer.setTotalCredit(
                        customer.getTotalCredit().add(onCredit));
                customer.setBalance(
                        customer.getTotalCredit()
                                .subtract(customer.getTotalPaid()));
                customerRepository.save(customer);

                log.info("Customer {} — partial credit: ₹{} | paid: ₹{} | balance: ₹{}",
                        customer.getName(), onCredit, paid, customer.getBalance());

                // ✅ FIXED: transaction variable correctly named and scoped
                Transaction partialTxn = Transaction.builder()
                        .customer(customer)
                        .bill(savedBill)
                        .type(Transaction.TransactionType.CREDIT)
                        .amount(onCredit)
                        .transactionDate(LocalDate.now())
                        .description("Partial bill: "
                                + savedBill.getBillNo()
                                + " | Paid: ₹" + paid
                                + " | Credit: ₹" + onCredit)
                        .balanceAfter(customer.getBalance())
                        .build();
                transactionRepository.save(partialTxn);
            }
        }
    }

    // ─────────────────────────────────────────────────────
    // Backward compatible overload (no partial amounts)
    // ─────────────────────────────────────────────────────
    public Bill createBill(Bill bill, List<BillItemRequest> itemRequests) {
        return createBill(bill, itemRequests, null, null);
    }

    // ─────────────────────────────────────────────────────
    // DELETE BILL
    // ─────────────────────────────────────────────────────
    @Transactional
    public void deleteBill(Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found: " + id));

        // Reverse credit for CREDIT or PARTIAL bills
        boolean isCreditOrPartial =
                Bill.PaymentStatus.CREDIT.equals(bill.getPaymentStatus())
                        || Bill.PaymentStatus.PARTIAL.equals(bill.getPaymentStatus());

        if (isCreditOrPartial && bill.getCustomer() != null) {

            Customer customer = customerRepository
                    .findById(bill.getCustomer().getId())
                    .orElse(null);

            if (customer != null) {

                // Find exact credit amount from transactions
                List<Transaction> billTxns =
                        transactionRepository.findByBillId(id);

                BigDecimal creditToReverse = billTxns.stream()
                        .filter(t -> Transaction.TransactionType.CREDIT
                                .equals(t.getType()))
                        .map(Transaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (creditToReverse.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal newCredit = customer.getTotalCredit()
                            .subtract(creditToReverse);
                    customer.setTotalCredit(
                            newCredit.compareTo(BigDecimal.ZERO) < 0
                                    ? BigDecimal.ZERO : newCredit);
                    // Recalculate balance
                    customer.setBalance(
                            customer.getTotalCredit()
                                    .subtract(customer.getTotalPaid()));
                    customerRepository.save(customer);

                    log.info("Bill deleted — reversed ₹{} credit for customer {}",
                            creditToReverse, customer.getName());
                }

                // Delete related transactions
                transactionRepository.deleteAll(billTxns);
            }
        }

        // Delete bill items first (prevents FK constraint error)
        billItemRepository.deleteAll(billItemRepository.findByBillId(id));

        // Delete bill
        billRepository.deleteById(id);
        log.info("Bill {} deleted successfully", id);
    }

    // ─────────────────────────────────────────────────────
    // DTO: BillItemRequest
    // ─────────────────────────────────────────────────────
    public record BillItemRequest(
            Long       productId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            String     lang,
            String     unit
    ) {
        // Backward compatible constructor
        public BillItemRequest(Long productId,
                               BigDecimal quantity,
                               BigDecimal unitPrice) {
            this(productId, quantity, unitPrice, "en", "piece");
        }

        public BillItemRequest(Long productId,
                               BigDecimal quantity,
                               BigDecimal unitPrice,
                               String lang) {
            this(productId, quantity, unitPrice, lang, "piece");
        }
    }
}