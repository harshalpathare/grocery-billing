package com.example.grocery_billing.service;

import com.example.grocery_billing.config.ShopContext;
import com.example.grocery_billing.entity.*;
import com.example.grocery_billing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository    customerRepository;
    private final TransactionRepository transactionRepository;
    private final BillRepository        billRepository;
    private final ShopRepository        shopRepository;
    private final CashFlowRepository    cashFlowRepository;

    // ── helpers ───────────────────────────────────────────
    private Long shopId() {
        return ShopContext.getShopId();
    }

    private Shop currentShop() {
        Long id = shopId();
        if (id == null) throw new RuntimeException("No shop in context");
        return shopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop not found: " + id));
    }

    // ─────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Customer> getAllActiveCustomers() {
        Long id = shopId();
        if (id != null) return customerRepository.findByShopIdAndActiveTrueOrderByNameAsc(id);
        return customerRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Customer> searchCustomers(String name) {
        if (name == null || name.trim().isEmpty()) return getAllActiveCustomers();
        Long id = shopId();
        if (id != null)
            return customerRepository.findByShopIdAndNameContainingIgnoreCaseAndActiveTrue(id, name.trim());
        return customerRepository.findByNameContainingIgnoreCaseAndActiveTrue(name.trim());
    }

    @Transactional
    public List<Customer> getCustomersWithPendingBalance() {
        Long id = shopId();
        List<Customer> all = (id != null)
                ? customerRepository.findByShopIdAndActiveTrueOrderByNameAsc(id)
                : customerRepository.findByActiveTrueOrderByNameAsc();

        for (Customer c : all) {
            BigDecimal credit = c.getTotalCredit() != null ? c.getTotalCredit() : BigDecimal.ZERO;
            BigDecimal paid   = c.getTotalPaid()   != null ? c.getTotalPaid()   : BigDecimal.ZERO;
            BigDecimal calc   = credit.subtract(paid);
            if (c.getBalance() == null || c.getBalance().compareTo(calc) != 0) {
                c.setBalance(calc);
                customerRepository.save(c);
            }
        }

        if (id != null)
            return customerRepository.findByShopIdAndBalanceGreaterThanAndActiveTrue(id, BigDecimal.ZERO);
        return customerRepository.findByBalanceGreaterThanAndActiveTrue(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public long countActiveCustomers() {
        Long id = shopId();
        if (id != null) return customerRepository.countByShopIdAndActiveTrue(id);
        return customerRepository.findByActiveTrueOrderByNameAsc().size();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPendingBalance() {
        Long id = shopId();
        if (id != null) return customerRepository.getTotalPendingBalanceByShop(id);
        return customerRepository.getTotalPendingBalance();
    }

    // ─────────────────────────────────────────────────────
    // SAVE / UPDATE
    // ─────────────────────────────────────────────────────

    @Transactional
    public Customer saveCustomer(Customer customer) {
        if (customer.getName()    != null) customer.setName(customer.getName().trim());
        if (customer.getPhone()   != null) customer.setPhone(customer.getPhone().trim());
        if (customer.getAddress() != null) customer.setAddress(customer.getAddress().trim());
        if (customer.getEmail()   != null) customer.setEmail(customer.getEmail().trim());

        Long sid = shopId();

        if (customer.getId() != null) {
            // Edit mode — check phone uniqueness within same shop
            boolean exists = (sid != null)
                    ? customerRepository.existsByPhoneAndShopIdAndIdNotAndActiveTrue(
                            customer.getPhone(), sid, customer.getId())
                    : customerRepository.existsByPhoneAndIdNotAndActiveTrue(
                            customer.getPhone(), customer.getId());
            if (exists)
                throw new RuntimeException("Phone " + customer.getPhone()
                        + " is already registered to another customer.");

            Customer existing = getCustomerById(customer.getId());
            existing.setName(customer.getName());
            existing.setPhone(customer.getPhone());
            existing.setAddress(customer.getAddress());
            existing.setEmail(customer.getEmail());
            existing.setGstin(customer.getGstin());
            existing.setNotes(customer.getNotes());
            if (customer.getCreditLimit() != null) {
                existing.setCreditLimit(customer.getCreditLimit());
            }
            if (existing.getShop() == null && sid != null) {
                existing.setShop(currentShop());
            }
            return customerRepository.save(existing);
        }

        // New customer — check uniqueness within shop
        boolean activeExists = (sid != null)
                ? customerRepository.existsByPhoneAndShopIdAndActiveTrue(
                        customer.getPhone(), sid)
                : customerRepository.existsByPhoneAndActiveTrue(customer.getPhone());
        if (activeExists)
            throw new RuntimeException("Phone " + customer.getPhone() + " is already registered.");

        // Reactivate soft-deleted within same shop
        Optional<Customer> deleted = (sid != null)
                ? customerRepository.findByPhoneAndShopId(customer.getPhone(), sid)
                        .filter(c -> !c.getActive())
                : customerRepository.findByPhoneAndActiveFalse(customer.getPhone());

        if (deleted.isPresent()) {
            Customer ex = deleted.get();
            ex.setName(customer.getName());
            ex.setAddress(customer.getAddress());
            ex.setEmail(customer.getEmail());
            ex.setNotes(customer.getNotes());
            ex.setActive(true);
            if (ex.getShop() == null && sid != null) {
                ex.setShop(currentShop());
            }
            return customerRepository.save(ex);
        }

        // Attach shop
        if (customer.getShop() == null && sid != null) {
            customer.setShop(currentShop());
        }
        return customerRepository.save(customer);
    }

    // ─────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────

    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);
        BigDecimal balance = customer.getBalance() != null ? customer.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(BigDecimal.ZERO) > 0)
            throw new RuntimeException("Cannot delete customer with pending balance ₹" + balance);
        customer.setActive(false);
        customerRepository.save(customer);
    }

    // ─────────────────────────────────────────────────────
    // CREDIT & PAYMENT
    // ─────────────────────────────────────────────────────

    @Transactional
    public Transaction addCredit(Long customerId, BigDecimal amount,
                                 Long billId, String description) {
        Customer customer = getCustomerById(customerId);
        if (customer.getTotalCredit() == null) customer.setTotalCredit(BigDecimal.ZERO);
        if (customer.getTotalPaid()   == null) customer.setTotalPaid(BigDecimal.ZERO);
        customer.setTotalCredit(customer.getTotalCredit().add(amount));
        customer.setBalance(customer.getTotalCredit().subtract(customer.getTotalPaid()));
        customerRepository.save(customer);

        Transaction txn = Transaction.builder()
                .customer(customer)
                .type(Transaction.TransactionType.CREDIT)
                .amount(amount)
                .transactionDate(LocalDate.now())
                .description(description != null ? description : "Credit purchase")
                .balanceAfter(customer.getBalance())
                .build();
        if (billId != null) {
            Bill b = new Bill(); b.setId(billId);
            txn.setBill(b);
        }
        return transactionRepository.save(txn);
    }

    @Transactional
    public Transaction recordPayment(Long customerId, BigDecimal amount, String description) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + customerId));

        if (customer.getTotalCredit() == null) customer.setTotalCredit(BigDecimal.ZERO);
        if (customer.getTotalPaid()   == null) customer.setTotalPaid(BigDecimal.ZERO);

        BigDecimal realBalance = customer.getTotalCredit().subtract(customer.getTotalPaid());
        customer.setBalance(realBalance);

        if (customer.getTotalCredit().compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("No credit history for " + customer.getName());
        if (realBalance.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException(customer.getName() + " has no pending balance.");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Payment amount must be greater than zero.");
        if (amount.compareTo(realBalance) > 0)
            throw new RuntimeException("Payment ₹" + amount + " exceeds balance ₹" + realBalance);

        customer.setTotalPaid(customer.getTotalPaid().add(amount));
        customer.setBalance(customer.getTotalCredit().subtract(customer.getTotalPaid()));
        customerRepository.saveAndFlush(customer);

        Transaction txn = Transaction.builder()
                .customer(customer)
                .type(Transaction.TransactionType.DEBIT)
                .amount(amount)
                .transactionDate(LocalDate.now())
                .description(description != null && !description.isBlank()
                        ? description : "Payment received")
                .balanceAfter(customer.getBalance())
                .build();
        txn = transactionRepository.saveAndFlush(txn);

        // If fully paid — mark all credit/partial bills as PAID
        if (customer.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
            Long sid = shopId();
            List<Bill> bills = (sid != null)
                    ? billRepository.findByShopIdAndCustomerIdOrderByBillDateDesc(sid, customerId)
                    : billRepository.findByCustomerIdOrderByBillDateDesc(customerId);
            bills.stream()
                    .filter(b -> b.getPaymentStatus() == Bill.PaymentStatus.CREDIT
                              || b.getPaymentStatus() == Bill.PaymentStatus.PARTIAL)
                    .forEach(b -> b.setPaymentStatus(Bill.PaymentStatus.PAID));
            billRepository.saveAll(bills);
        }
        
        // ✅ Automatic Cash Flow (IN)
        CashFlow cf = new CashFlow();
        cf.setShop(customer.getShop());
        cf.setTransactionDate(LocalDate.now());
        cf.setType(CashFlow.TransactionType.IN);
        cf.setAmount(amount);
        cf.setCategory("Customer Payment");
        cf.setDescription("Payment from: " + customer.getName());
        cashFlowRepository.save(cf);

        return txn;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(Long customerId) {
        return transactionRepository
                .findByCustomerIdOrderByTransactionDateDescCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public List<Bill> getPurchaseHistory(Long customerId) {
        Long sid = shopId();
        if (sid != null) {
            return billRepository.findByShopIdAndCustomerIdOrderByBillDateDesc(sid, customerId);
        }
        return billRepository.findByCustomerIdOrderByBillDateDesc(customerId);
    }

    public static class CustomerReactivatedException extends RuntimeException {
        public CustomerReactivatedException(String message) { super(message); }
    }
}
