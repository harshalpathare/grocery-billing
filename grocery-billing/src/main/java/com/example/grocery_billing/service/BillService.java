package com.example.grocery_billing.service;

import com.example.grocery_billing.config.ShopContext;
import com.example.grocery_billing.entity.*;
import com.example.grocery_billing.repository.*;
import com.example.grocery_billing.service.ShopFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final ShopRepository        shopRepository;
    private final CashFlowRepository    cashFlowRepository;
    private final ShopFeatureService    shopFeatureService;

    // ── helpers ───────────────────────────────────────────
    private Long shopId() { return ShopContext.getShopId(); }

    private Shop currentShop() {
        Long id = shopId();
        if (id == null) throw new RuntimeException("No shop in context");
        return shopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop not found: " + id));
    }

    // ─────────────────────────────────────────────────────
    // READ OPERATIONS
    // ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Bill> getAllBills() {
        Long id = shopId();
        if (id != null) return billRepository.findByShopIdOrderByCreatedAtDesc(id);
        return billRepository.findAll(Sort.by(Sort.Direction.DESC, "billDate", "id"));
    }

    @Transactional(readOnly = true)
    public Bill getBillById(Long id) {
        return billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Bill> getBillsByDate(LocalDate date) {
        Long sid = shopId();
        if (sid != null) return billRepository.findByShopIdAndBillDateOrderByCreatedAtDesc(sid, date);
        return billRepository.findByBillDateOrderByCreatedAtDesc(date);
    }

    @Transactional(readOnly = true)
    public List<Bill> getBillsBetween(LocalDate start, LocalDate end) {
        Long sid = shopId();
        if (sid != null) return billRepository.findByShopIdAndBillDateBetweenOrderByBillDateDesc(sid, start, end);
        return billRepository.findByBillDateBetweenOrderByBillDateDesc(start, end);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTodaySales() {
        Long sid = shopId();
        if (sid != null) return billRepository.getTotalSalesByDateAndShop(sid, LocalDate.now());
        return billRepository.getTotalSalesByDate(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public long getTodayBillCount() {
        Long sid = shopId();
        if (sid != null) return billRepository.countByShopIdAndBillDate(sid, LocalDate.now());
        return billRepository.countByBillDate(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public BigDecimal getMonthlySales(int year, int month) {
        Long sid = shopId();
        if (sid != null) return billRepository.getTotalSalesByMonthAndShop(sid, year, month);
        return billRepository.getTotalSalesByMonth(year, month);
    }

    // ─────────────────────────────────────────────────────
    // AUTO GENERATE BILL NUMBER
    // ─────────────────────────────────────────────────────
    public String generateBillNumber() {
        String year  = String.valueOf(LocalDate.now().getYear());
        Long   sid   = shopId();
        int    serial = 1;

        Optional<String> lastBillNo = (sid != null)
                ? billRepository.findLastBillNoForYearAndShop(sid, year)
                : billRepository.findLastBillNoForYear(year);

        if (lastBillNo.isPresent()) {
            try {
                String[] parts = lastBillNo.get().split("-");
                serial = Integer.parseInt(parts[parts.length - 1]) + 1;
            } catch (Exception e) { serial = 1; }
        }

        String candidate;
        do {
            candidate = String.format("BILL-%s-%04d", year, serial++);
        } while (sid != null
                ? billRepository.existsByBillNoAndShopId(candidate, sid)
                : billRepository.existsByBillNo(candidate));

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

        // Attach shop
        if (bill.getShop() == null) {
            bill.setShop(currentShop());
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
            item.setIsReturn(req.isReturn() != null ? req.isReturn() : false);
            item.calculateItemTotal();
            bill.addBillItem(item);

            // Update stock
            if (product.getStockQty() != null) {
                BigDecimal currentStock = product.getStockQty();
                BigDecimal newStock;
                if (item.getIsReturn()) {
                    // Restock returned items
                    newStock = currentStock.add(billedQuantity);
                } else {
                    // Reduce stock for sold items
                    newStock = currentStock.subtract(billedQuantity);
                }
                product.setStockQty(newStock.max(BigDecimal.ZERO));
                productRepository.save(product);
            }
        }

        // Step 4: Calculate totals
        bill.calculateTotals();

        // Step 4.5: Validate Customer Credit Limit if buying on credit
        if (customerId != null && (Bill.PaymentStatus.CREDIT.equals(bill.getPaymentStatus()) || Bill.PaymentStatus.PARTIAL.equals(bill.getPaymentStatus()))) {
            Customer c = customerRepository.findById(customerId).orElse(null);
            if (c != null && c.getCreditLimit() != null && c.getCreditLimit().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentBal = c.getBalance() != null ? c.getBalance() : BigDecimal.ZERO;
                BigDecimal billTotal = bill.getTotalAmount() != null ? bill.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal creditPart = Bill.PaymentStatus.CREDIT.equals(bill.getPaymentStatus())
                        ? billTotal
                        : (creditAmount != null ? creditAmount : billTotal.subtract(paidAmount != null ? paidAmount : BigDecimal.ZERO));

                if (creditPart.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal prospectiveBalance = currentBal.add(creditPart);
                    if (prospectiveBalance.compareTo(c.getCreditLimit()) > 0) {
                        BigDecimal excess = prospectiveBalance.subtract(c.getCreditLimit());
                        String limitStr = String.format("%.2f", c.getCreditLimit());
                        String duesStr = String.format("%.2f", currentBal);
                        String creditStr = String.format("%.2f", creditPart);
                        String excessStr = String.format("%.2f", excess);

                        throw new RuntimeException("Credit Limit Exceeded for " + c.getName()
                                + " | Limit: ₹" + limitStr
                                + " | Dues: ₹" + duesStr
                                + " | Attempted Credit: ₹" + creditStr
                                + " (Exceeds by ₹" + excessStr + "). Please collect cash or digital payment.");
                    }
                }
            }
        }

        // Step 5: Save bill
        Bill savedBill = billRepository.save(bill);
        log.info("Bill saved: {} | Status: {} | Amount: {} | CustomerId: {}",
                savedBill.getBillNo(),
                savedBill.getPaymentStatus(),
                savedBill.getTotalAmount(),
                customerId);

        // Step 6: Update customer balance and loyalty points
        if (customerId != null) {
            updateCustomerCredit(savedBill, customerId, paidAmount, creditAmount);
            
            // Loyalty Points Logic: 1 point for every ₹100 spent (total amount)
            // Only runs if Loyalty Points feature is enabled for this shop
            Map<String, Boolean> features = shopFeatureService.getFeaturesForCurrentShop();
            boolean loyaltyEnabled = Boolean.TRUE.equals(features.get("enable_loyalty"));
            if (loyaltyEnabled && savedBill.getTotalAmount() != null && savedBill.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
                int earnedPoints = savedBill.getTotalAmount().divide(new BigDecimal("100"), 0, RoundingMode.DOWN).intValue();
                if (earnedPoints > 0) {
                    Customer customer = customerRepository.findById(customerId).orElse(null);
                    if (customer != null) {
                        int currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;
                        customer.setLoyaltyPoints(currentPoints + earnedPoints);
                        if (customer.getShop() == null && shopId() != null) {
                            customer.setShop(shopRepository.findById(shopId()).orElse(null));
                        }
                        customerRepository.save(customer);
                        log.info("Customer {} earned {} loyalty points (Total: {})", customer.getName(), earnedPoints, customer.getLoyaltyPoints());
                    }
                }
            }
        }
        bill.setCreatedAt(LocalDateTime.now());
        
        // Step 7: Automatic Cash Flow logging
        if (Bill.PaymentStatus.PAID.equals(savedBill.getPaymentStatus())) {
            // Full payment (CASH, UPI, CASH_UPI, CARD, etc.)
            createCashFlowIn(savedBill.getTotalAmount(), "Sales (Bill #" + savedBill.getBillNo() + ")");
        } else if (Bill.PaymentStatus.PARTIAL.equals(savedBill.getPaymentStatus()) && paidAmount != null && paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Partial payment
            createCashFlowIn(paidAmount, "Partial Sales (Bill #" + savedBill.getBillNo() + ")");
        }

        return savedBill;
    }

    private void createCashFlowIn(BigDecimal amount, String note) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        CashFlow cf = new CashFlow();
        cf.setShop(currentShop());
        cf.setTransactionDate(LocalDate.now());
        cf.setType(CashFlow.TransactionType.IN);
        cf.setAmount(amount);
        cf.setCategory("Sales");
        cf.setDescription(note);
        cashFlowRepository.save(cf);
    }
    public List<Bill> getRecentBills(int limit) {
        Long sid = shopId();
        if (sid != null) return billRepository.findTop10ByShopIdOrderByCreatedAtDesc(sid)
                .stream().limit(limit).toList();
        return billRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().limit(limit).toList();
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
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
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
            String     unit,
            Boolean    isReturn
    ) {
        // Backward compatible constructor
        public BillItemRequest(Long productId,
                               BigDecimal quantity,
                               BigDecimal unitPrice) {
            this(productId, quantity, unitPrice, "en", "piece", false);
        }

        public BillItemRequest(Long productId,
                               BigDecimal quantity,
                               BigDecimal unitPrice,
                               String lang) {
            this(productId, quantity, unitPrice, lang, "piece", false);
        }
        
        public BillItemRequest(Long productId,
                               BigDecimal quantity,
                               BigDecimal unitPrice,
                               String lang,
                               String unit) {
            this(productId, quantity, unitPrice, lang, unit, false);
        }
    }
}