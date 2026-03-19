package com.example.grocery_billing.service;


import com.example.grocery_billing.entity.Customer;
import com.example.grocery_billing.entity.Transaction;
import com.example.grocery_billing.repository.CustomerRepository;
import com.example.grocery_billing.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * CUSTOMER SERVICE
 *
 * Handles all customer and credit/udhari business logic.
 *
 * Key concepts:
 *  - CREDIT transaction = customer took goods, owes us money
 *  - DEBIT  transaction = customer paid, reduces their balance
 *  - balance = totalCredit - totalPaid
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
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
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Customer> searchCustomers(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getAllActiveCustomers();
        }
        return customerRepository.findByNameContainingIgnoreCaseAndActiveTrue(name.trim());
    }

    @Transactional(readOnly = true)
    // ✅ NEW — refresh all balances before returning
    public List<Customer> getCustomersWithPendingBalance() {
        // First fix any customers where balance is out of sync
        List<Customer> allCustomers = customerRepository
                .findByActiveTrueOrderByNameAsc();

        for (Customer c : allCustomers) {
            BigDecimal credit = c.getTotalCredit() != null
                    ? c.getTotalCredit() : BigDecimal.ZERO;
            BigDecimal paid   = c.getTotalPaid() != null
                    ? c.getTotalPaid() : BigDecimal.ZERO;
            BigDecimal calculatedBalance = credit.subtract(paid);

            // If balance is out of sync, fix it
            if (c.getBalance() == null ||
                    c.getBalance().compareTo(calculatedBalance) != 0) {
                c.setBalance(calculatedBalance);
                customerRepository.save(c);
            }
        }

        // Now return customers with balance > 0
        return customerRepository
                .findByBalanceGreaterThanAndActiveTrue(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public long countActiveCustomers() {
        return customerRepository.findByActiveTrueOrderByNameAsc().size();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPendingBalance() {
        return customerRepository.getTotalPendingBalance();
    }

    // ─────────────────────────────────────────────────────
    // SAVE / UPDATE CUSTOMER
    // ─────────────────────────────────────────────────────

    public Customer saveCustomer(Customer customer) {

        // Trim fields
        if (customer.getName() != null)
            customer.setName(customer.getName().trim());
        if (customer.getPhone() != null)
            customer.setPhone(customer.getPhone().trim());
        if (customer.getAddress() != null)
            customer.setAddress(customer.getAddress().trim());
        if (customer.getEmail() != null)
            customer.setEmail(customer.getEmail().trim());

        // ── EDIT MODE ─────────────────────────────────────
        if (customer.getId() != null) {
            // Check phone is not taken by another ACTIVE customer
            boolean phoneExists = customerRepository
                    .existsByPhoneAndIdNotAndActiveTrue(
                            customer.getPhone(), customer.getId());
            if (phoneExists) {
                throw new RuntimeException(
                        "Phone number " + customer.getPhone()
                                + " is already registered to another customer.");
            }
            return customerRepository.save(customer);
        }

        // ── NEW CUSTOMER MODE ──────────────────────────────

        // Step 1: Check if phone belongs to an ACTIVE customer
        boolean activeExists = customerRepository
                .existsByPhoneAndActiveTrue(customer.getPhone());
        if (activeExists) {
            throw new RuntimeException(
                    "Phone number " + customer.getPhone()
                            + " is already registered.");
        }

        // Step 2: Check if phone belongs to a DELETED (inactive) customer
        // If yes — reactivate that record instead of creating a new one
        Optional<Customer> deletedCustomer =
                customerRepository.findByPhoneAndActiveFalse(customer.getPhone());

        if (deletedCustomer.isPresent()) {
            // Reactivate the old record with updated details
            com.example.grocery_billing.entity.Customer existing =
                    deletedCustomer.get();
            existing.setName(customer.getName());
            existing.setPhone(customer.getPhone());
            existing.setAddress(customer.getAddress());
            existing.setEmail(customer.getEmail());
            existing.setNotes(customer.getNotes());
            existing.setActive(true);
            // Keep old credit/balance history intact
            return customerRepository.save(existing);
        }

        // Step 3: Completely new customer — just save
        return customerRepository.save(customer);
    }

    // ── Custom exception for reactivation ─────────────────
// We use this to signal "success but reactivated" to controller
    public static class CustomerReactivatedException extends RuntimeException {
        public CustomerReactivatedException(String message) {
            super(message);
        }
    }

    // ─────────────────────────────────────────────────────
    // SOFT DELETE
    // ─────────────────────────────────────────────────────

    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);
        if (customer.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException(
                    "Cannot delete customer with pending balance of ₹"
                            + customer.getBalance() + ". Clear dues first.");
        }
        customer.setActive(false);
        customerRepository.save(customer);
    }

    // ─────────────────────────────────────────────────────
    // CREDIT OPERATIONS (Udhari system)
    // ─────────────────────────────────────────────────────

    /**
     * Record a credit transaction (customer takes goods on credit).
     * Called automatically when a bill is saved with CREDIT payment status.
     *
     * @param customerId  which customer
     * @param amount      how much credit taken
     * @param billId      which bill (optional)
     * @param description note about the transaction
     */
    public Transaction addCredit(Long customerId, BigDecimal amount,
                                 Long billId, String description) {
        Customer customer = getCustomerById(customerId);

        // Update customer totals
        customer.setTotalCredit(customer.getTotalCredit().add(amount));
        customer.updateBalance();  // recalculates balance = totalCredit - totalPaid
        customerRepository.save(customer);

        // Record the transaction
        Transaction transaction = Transaction.builder()
                .customer(customer)
                .type(Transaction.TransactionType.CREDIT)
                .amount(amount)
                .transactionDate(LocalDate.now())
                .description(description != null ? description : "Credit purchase")
                .balanceAfter(customer.getBalance())
                .build();

        // Link to bill if provided
        if (billId != null) {
            // We just set the bill id reference using a proxy
            // (full Bill object not needed here)
            com.example.grocery_billing.entity.Bill bill =
                    new com.example.grocery_billing.entity.Bill();
            bill.setId(billId);
            transaction.setBill(bill);
        }

        return transactionRepository.save(transaction);
    }

    /**
     * Record a payment (customer pays back money).
     * Called from the "Receive Payment" form.
     *
     * @param customerId  which customer
     * @param amount      how much they paid
     * @param description e.g. "Cash payment", "UPI transfer"
     */
    public Transaction recordPayment(Long customerId,
                                     BigDecimal amount,
                                     String description) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException(
                        "Customer not found: " + customerId));

        // Initialize nulls
        if (customer.getTotalCredit() == null)
            customer.setTotalCredit(BigDecimal.ZERO);
        if (customer.getTotalPaid() == null)
            customer.setTotalPaid(BigDecimal.ZERO);
        if (customer.getBalance() == null)
            customer.setBalance(BigDecimal.ZERO);

        // Recalculate balance freshly before checking
        BigDecimal currentBalance = customer.getTotalCredit()
                .subtract(customer.getTotalPaid());
        customer.setBalance(currentBalance);

        // Cannot pay more than what is owed
        if (amount.compareTo(currentBalance) > 0) {
            throw new RuntimeException(
                    "Payment ₹" + amount
                            + " exceeds pending balance ₹" + currentBalance
                            + ". Please enter correct amount.");
        }

        // Add to total paid
        customer.setTotalPaid(customer.getTotalPaid().add(amount));

        // Recalculate balance
        customer.setBalance(
                customer.getTotalCredit()
                        .subtract(customer.getTotalPaid()));

        customerRepository.save(customer);

        Transaction txn = Transaction.builder()
                .customer(customer)
                .type(Transaction.TransactionType.DEBIT)
                .amount(amount)
                .transactionDate(LocalDate.now())
                .description(description != null
                        ? description : "Payment received")
                .balanceAfter(customer.getBalance())
                .build();

        return transactionRepository.save(txn);
    }
    // ─────────────────────────────────────────────────────
    // TRANSACTION HISTORY
    // ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(Long customerId) {
        return transactionRepository
                .findByCustomerIdOrderByTransactionDateDescCreatedAtDesc(customerId);
    }
}