package com.example.grocery_billing.entity;



import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BILL ENTITY
 * Maps to the 'bills' table.
 * Represents one complete invoice/receipt.
 *
 * Real-world: Customer Ramesh buys Sugar + Rice + Oil.
 * One Bill is created, with 3 BillItems inside it.
 */
@Entity
@Table(name = "bills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Bill number ───────────────────────────────────────
    // e.g. "BILL-2024-001", "BILL-2024-002" etc.
    @Column(name = "bill_no", unique = true, length = 30)
    private String billNo;

    // ── Date ─────────────────────────────────────────────
    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;

    // ── Customer link (MANY bills belong to ONE customer) ─
    // @ManyToOne = Many bills → One customer
    // @JoinColumn = the foreign key column name in 'bills' table
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")  // nullable = walk-in customer (no account)
    @ToString.Exclude
    private Customer customer;

    // ── GST flag ─────────────────────────────────────────
    // true  = GST bill (with GST breakdown)
    // false = simple bill (no GST)
    @Column(name = "is_gst", nullable = false)
    private Boolean isGst = false;

    // ── Amount fields ─────────────────────────────────────
    @Column(name = "subtotal", precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "gst_amount", precision = 12, scale = 2)
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "transport_cost", precision = 10, scale = 2)
    private BigDecimal transportCost = BigDecimal.ZERO;

    @Column(name = "extra_cost", precision = 10, scale = 2)
    private BigDecimal extraCost = BigDecimal.ZERO;

    @Column(name = "discount", precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    // total = subtotal + gstAmount + transportCost + extraCost - discount
    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // ── Payment status ────────────────────────────────────
    // PAID, CREDIT (udhari), PARTIAL
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod = "CASH";  // CASH, UPI, CARD

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    // ── Relationship: Bill has MANY BillItems ─────────────
    // cascade = ALL: saving a Bill automatically saves its items
    // orphanRemoval = true: if you remove an item from the list, it's deleted from DB
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @Builder.Default
    private List<BillItem> billItems = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (billDate == null) billDate = LocalDate.now();
    }

    // ── Helper: add a BillItem to this bill ───────────────
    public void addBillItem(BillItem item) {
        billItems.add(item);
        item.setBill(this);
    }

    // ── Helper: recalculate totals ────────────────────────
    public void calculateTotals() {
        this.subtotal = billItems.stream()
                .map(BillItem::getItemTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (Boolean.TRUE.equals(this.isGst)) {
            this.gstAmount = billItems.stream()
                    .map(item -> item.getItemTotal()
                            .multiply(item.getGstPercent())
                            .divide(BigDecimal.valueOf(100)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            this.gstAmount = BigDecimal.ZERO;
        }

        this.totalAmount = subtotal
                .add(gstAmount)
                .add(transportCost != null ? transportCost : BigDecimal.ZERO)
                .add(extraCost != null ? extraCost : BigDecimal.ZERO)
                .subtract(discount != null ? discount : BigDecimal.ZERO);
    }

    // ── Enum for payment status ───────────────────────────
    public enum PaymentStatus {
        PAID, CREDIT, PARTIAL
    }
}