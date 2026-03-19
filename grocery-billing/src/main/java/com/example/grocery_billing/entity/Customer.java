package com.example.grocery_billing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CUSTOMER ENTITY
 * Maps to the 'customers' table.
 *
 * Real-world example:
 *   Ramesh Patil, phone: 9876543210
 *   He has taken ₹500 of items on credit (udhari),
 *   paid back ₹200, so balance = ₹300 still pending.
 */
@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Customer name is required")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    // Phone is unique — no two customers with same number
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter valid 10-digit Indian mobile number")
    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Size(max = 255)
    private String address;

    @Email(message = "Enter valid email")
    @Size(max = 100)
    @Column(length = 100)
    private String email;

    // ── Credit/Udhari tracking ────────────────────────────
    // total_credit = total amount taken on credit (ever)
    @Column(name = "total_credit", precision = 12, scale = 2)
    private BigDecimal totalCredit = BigDecimal.ZERO;

    // total_paid = total amount paid back
    @Column(name = "total_paid", precision = 12, scale = 2)
    private BigDecimal totalPaid = BigDecimal.ZERO;

    // balance = totalCredit - totalPaid (still pending)
    // This is calculated automatically — see updateBalance()
    @Column(name = "balance", precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Relationships ─────────────────────────────────────
    // One customer can have MANY bills
    // mappedBy = "customer" means the 'customer' field in Bill owns this relationship
    // cascade = ALL means if customer is deleted, their bills are also deleted
    // fetch = LAZY means bills are NOT loaded unless you specifically ask for them
    //         (performance optimization — don't load 1000 bills just to show customer name)
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude  // Lombok: prevents infinite loop in toString
    @Builder.Default
    private List<Bill> bills = new ArrayList<>();

    // One customer can have MANY transactions (credit/debit history)
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ── Helper method ─────────────────────────────────────
    /**
     * Call this whenever credit or payment changes.
     * Keeps balance in sync automatically.
     */
    public void updateBalance() {
        BigDecimal credit = this.totalCredit != null
                ? this.totalCredit : BigDecimal.ZERO;
        BigDecimal paid   = this.totalPaid != null
                ? this.totalPaid : BigDecimal.ZERO;
        this.balance = credit.subtract(paid);
    }
}