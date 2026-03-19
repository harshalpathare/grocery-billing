package com.example.grocery_billing.entity;



import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * BILL ITEM ENTITY
 * Maps to the 'bill_items' table.
 * Represents ONE line in a bill.
 *
 * Real-world: In Ramesh's bill:
 *   Line 1: Sugar × 2 kg @ ₹45 = ₹90   ← this is one BillItem
 *   Line 2: Rice  × 5 kg @ ₹60 = ₹300  ← this is another BillItem
 */
@Entity
@Table(name = "bill_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Which bill does this item belong to? ──────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    @ToString.Exclude
    private Bill bill;

    // ── Which product is this? ────────────────────────────
    @ManyToOne(fetch = FetchType.EAGER)  // EAGER: always load product details
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // ── Product name snapshot ─────────────────────────────
    // We store the name at time of billing.
    // Why? Because if product name changes later, old bills should
    // still show the original name.
    @Column(name = "product_name_snapshot", length = 100)
    private String productNameSnapshot;

    // ── Quantity ──────────────────────────────────────────
    @NotNull
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    // ── Price at time of billing ──────────────────────────
    // Snapshot again — price might change, bill stays correct
    @NotNull
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // ── GST % applied to this item ────────────────────────
    @Column(name = "gst_percent", precision = 5, scale = 2)
    private BigDecimal gstPercent = BigDecimal.ZERO;

    // ── Calculated total for this line ────────────────────
    // item_total = quantity × unit_price
    // (GST is calculated separately on the Bill level)
    @Column(name = "item_total", precision = 12, scale = 2)
    private BigDecimal itemTotal = BigDecimal.ZERO;

    // ── Auto-calculate itemTotal before saving ────────────
    @PrePersist
    @PreUpdate
    public void calculateItemTotal() {
        if (quantity != null && unitPrice != null) {
            this.itemTotal = quantity.multiply(unitPrice);
        }
    }
}