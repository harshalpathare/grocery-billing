package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Customer;
import com.example.grocery_billing.entity.Transaction;
import com.example.grocery_billing.repository.CustomerRepository;
import com.example.grocery_billing.repository.TransactionRepository;
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
@Slf4j  // ✅ ADDED — fixes log.info() compile error
public class CustomerService {

    private final CustomerRepository    customerRepository;
    private final TransactionRepository transactionRepository;

    // ─────────────────────────────────────────────────────
    // READ OPERATIONS
    // ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Customer> getAllActiveCustomers() {
        return customerRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Customer not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Customer> searchCustomers(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getAllActiveCustomers();
        }
        return customerRepository
                .findByNameContainingIgnoreCaseAndActiveTrue(
                        name.trim());
    }

    // ✅ FIXED: removed @Transactional(readOnly=true)
    // because we do save() inside to fix out-of-sync balances
    @Transactional
    public List<Customer> getCustomersWithPendingBalance() {
        List<Customer> allCustomers = customerRepository
                .findByActiveTrueOrderByNameAsc();

        for (Customer c : allCustomers) {
            BigDecimal credit = c.getTotalCredit() != null
                    ? c.getTotalCredit() : BigDecimal.ZERO;
            BigDecimal paid   = c.getTotalPaid()   != null
                    ? c.getTotalPaid()   : BigDecimal.ZERO;
            BigDecimal calculatedBalance = credit.subtract(paid);

            if (c.getBalance() == null ||
                    c.getBalance().compareTo(
                            calculatedBalance) != 0) {
                c.setBalance(calculatedBalance);
                customerRepository.save(c);
            }
        }

        return customerRepository
                .findByBalanceGreaterThanAndActiveTrue(
                        BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public long countActiveCustomers() {
        return customerRepository
                .findByActiveTrueOrderByNameAsc().size();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPendingBalance() {
        return customerRepository.getTotalPendingBalance();
    }

    // ─────────────────────────────────────────────────────
    // SAVE / UPDATE CUSTOMER
    // ─────────────────────────────────────────────────────
    @Transactional
    public Customer saveCustomer(Customer customer) {

        if (customer.getName() != null)
            customer.setName(customer.getName().trim());
        if (customer.getPhone() != null)
            customer.setPhone(customer.getPhone().trim());
        if (customer.getAddress() != null)
            customer.setAddress(customer.getAddress().trim());
        if (customer.getEmail() != null)
            customer.setEmail(customer.getEmail().trim());

        // ── EDIT MODE ──────────────────────────────────
        if (customer.getId() != null) {
            boolean phoneExists = customerRepository
                    .existsByPhoneAndIdNotAndActiveTrue(
                            customer.getPhone(),
                            customer.getId());
            if (phoneExists) {
                throw new RuntimeException(
                        "Phone " + customer.getPhone()
                                + " is already registered to another customer.");
            }
            return customerRepository.save(customer);
        }

        // ── NEW CUSTOMER ───────────────────────────────
        boolean activeExists = customerRepository
                .existsByPhoneAndActiveTrue(customer.getPhone());
        if (activeExists) {
            throw new RuntimeException(
                    "Phone " + customer.getPhone()
                            + " is already registered.");
        }

        // Reactivate soft-deleted customer if same phone
        Optional<Customer> deletedCustomer =
                customerRepository.findByPhoneAndActiveFalse(
                        customer.getPhone());

        if (deletedCustomer.isPresent()) {
            Customer existing = deletedCustomer.get();
            existing.setName(customer.getName());
            existing.setPhone(customer.getPhone());
            existing.setAddress(customer.getAddress());
            existing.setEmail(customer.getEmail());
            existing.setNotes(customer.getNotes());
            existing.setActive(true);
            return customerRepository.save(existing);
        }

        return customerRepository.save(customer);
    }

    // ─────────────────────────────────────────────────────
    // SOFT DELETE
    // ─────────────────────────────────────────────────────
    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);

        BigDecimal balance = customer.getBalance() != null
                ? customer.getBalance() : BigDecimal.ZERO;

        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException(
                    "Cannot delete customer with pending balance ₹"
                            + balance + ". Clear dues first.");
        }
        customer.setActive(false);
        customerRepository.save(customer);
    }

    // ─────────────────────────────────────────────────────
    // ADD CREDIT (called from BillService)
    // ─────────────────────────────────────────────────────
    @Transactional
    public Transaction addCredit(Long customerId,
                                 BigDecimal amount,
                                 Long billId,
                                 String description) {
        Customer customer = getCustomerById(customerId);

        if (customer.getTotalCredit() == null)
            customer.setTotalCredit(BigDecimal.ZERO);
        if (customer.getTotalPaid() == null)
            customer.setTotalPaid(BigDecimal.ZERO);

        customer.setTotalCredit(
                customer.getTotalCredit().add(amount));
        customer.setBalance(
                customer.getTotalCredit()
                        .subtract(customer.getTotalPaid()));
        customerRepository.save(customer);

        Transaction transaction = Transaction.builder()
                .customer(customer)
                .type(Transaction.TransactionType.CREDIT)
                .amount(amount)
                .transactionDate(LocalDate.now())
                .description(description != null
                        ? description : "Credit purchase")
                .balanceAfter(customer.getBalance())
                .build();

        if (billId != null) {
            com.example.grocery_billing.entity.Bill bill =
                    new com.example.grocery_billing.entity.Bill();
            bill.setId(billId);
            transaction.setBill(bill);
        }

        return transactionRepository.save(transaction);
    }

    // ─────────────────────────────────────────────────────
    // RECORD PAYMENT (customer pays back)
    // ─────────────────────────────────────────────────────
    @Transactional
    public Transaction recordPayment(Long customerId,
                                     BigDecimal amount,
                                     String description) {

        Customer customer = customerRepository
                .findById(customerId)
                .orElseThrow(() -> new RuntimeException(
                        "Customer not found: " + customerId));

        // Initialize nulls
        if (customer.getTotalCredit() == null)
            customer.setTotalCredit(BigDecimal.ZERO);
        if (customer.getTotalPaid() == null)
            customer.setTotalPaid(BigDecimal.ZERO);

        // ✅ ALWAYS recalculate balance fresh from DB
        // Never trust the stored balance field
        BigDecimal realBalance = customer.getTotalCredit()
                .subtract(customer.getTotalPaid());
        customer.setBalance(realBalance);

        log.info("recordPayment — {} | realBalance: {} | paying: {}",
                customer.getName(), realBalance, amount);

        // ✅ GUARD 1: No credit exists at all
        if (customer.getTotalCredit()
                .compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "Cannot record payment — "
                            + customer.getName()
                            + " has no credit history. "
                            + "Create a credit bill first.");
        }

        // ✅ GUARD 2: Balance is zero or negative
        if (realBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    customer.getName()
                            + " has no pending balance. "
                            + "Current balance: ₹" + realBalance);
        }

        // ✅ GUARD 3: Amount must be positive
        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "Payment amount must be greater than zero.");
        }

        // ✅ GUARD 4: Cannot overpay
        if (amount.compareTo(realBalance) > 0) {
            throw new RuntimeException(
                    "Payment ₹" + amount
                            + " exceeds balance ₹" + realBalance
                            + ". Maximum: ₹" + realBalance);
        }

        // All checks passed — record payment
        customer.setTotalPaid(
                customer.getTotalPaid().add(amount));
        customer.setBalance(
                customer.getTotalCredit()
                        .subtract(customer.getTotalPaid()));

        customerRepository.saveAndFlush(customer);

        log.info("Payment OK — new balance: {}",
                customer.getBalance());

        Transaction txn = Transaction.builder()
                .customer(customer)
                .type(Transaction.TransactionType.DEBIT)
                .amount(amount)
                .transactionDate(LocalDate.now())
                .description(description != null
                        && !description.isBlank()
                        ? description
                        : "Payment received")
                .balanceAfter(customer.getBalance())
                .build();

        return transactionRepository.saveAndFlush(txn);
    }

    // ─────────────────────────────────────────────────────
    // TRANSACTION HISTORY
    // ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(Long customerId) {
        return transactionRepository
                .findByCustomerIdOrderByTransactionDateDescCreatedAtDesc(
                        customerId);
    }

    // ─────────────────────────────────────────────────────
    // INNER CLASS — kept for backward compatibility
    // ─────────────────────────────────────────────────────
    public static class CustomerReactivatedException
            extends RuntimeException {
        public CustomerReactivatedException(String message) {
            super(message);
        }
    }
}