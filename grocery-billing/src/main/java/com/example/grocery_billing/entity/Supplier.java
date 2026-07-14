package com.example.grocery_billing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SUPPLIER ENTITY
 * Represents a vendor/wholesaler we buy products from.
 */
@Entity
@Table(name = "suppliers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Shop (multi-tenant) ───────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    @ToString.Exclude
    private Shop shop;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(length = 15)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 200)
    private String address;

    // GSTIN of supplier (for ITC claims)
    @Column(name = "gstin", length = 20)
    private String gstin;

    // Total amount we owe this supplier
    @Column(name = "total_payable", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalPayable = BigDecimal.ZERO;

    // Total amount we have paid
    @Column(name = "total_paid", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalPaid = BigDecimal.ZERO;

    // Balance = totalPayable - totalPaid
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 500)
    private String notes;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (totalPayable == null) totalPayable = BigDecimal.ZERO;
        if (totalPaid    == null) totalPaid    = BigDecimal.ZERO;
        if (balance      == null) balance      = BigDecimal.ZERO;
    }

    public void updateBalance() {
        this.balance = (this.totalPayable != null
                ? this.totalPayable : BigDecimal.ZERO)
                .subtract(this.totalPaid != null
                        ? this.totalPaid : BigDecimal.ZERO);
    }
}