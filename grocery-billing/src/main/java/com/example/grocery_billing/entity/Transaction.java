package com.example.grocery_billing.entity;



import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TRANSACTION ENTITY
 * Maps to the 'transactions' table.
 * Records every credit (udhari) and payment event.
 *
 * Real-world examples:
 *   CREDIT: Ramesh took goods worth ₹500 on credit → type=CREDIT, amount=500
 *   DEBIT:  Ramesh paid back ₹200 cash           → type=DEBIT,  amount=200
 */
@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Which customer? ───────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    private Customer customer;

    // ── Related bill (optional) ───────────────────────────
    // A payment transaction may not be linked to a specific bill
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    @ToString.Exclude
    private Bill bill;

    // ── Transaction type ──────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private TransactionType type;

    // ── Amount ────────────────────────────────────────────
    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // ── Date of transaction ───────────────────────────────
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    // ── Description ───────────────────────────────────────
    // e.g. "Monthly settlement", "Cash payment", "Festival advance"
    @Size(max = 300)
    private String description;

    // ── Running balance after this transaction ────────────
    // Stored for quick display in history
    @Column(name = "balance_after", precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (transactionDate == null) transactionDate = LocalDate.now();
    }

    // ── Enum ──────────────────────────────────────────────
    public enum TransactionType {
        CREDIT,   // Customer took goods — they owe us money
        DEBIT     // Customer paid — reduces their balance
    }
}