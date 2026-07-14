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

    // ── Quantity shown to the customer ──────────────────
    // For gram sales, this remains the entered gram amount.
    @Column(name = "display_quantity", precision = 10, scale = 3)
    private BigDecimal displayQuantity;

    // ── Price at time of billing ──────────────────────────
    // Snapshot again — price might change, bill stays correct
    @NotNull
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // ── Billing unit used for display/conversion ─────────
    // Examples: piece, kg, gram, litre, ml
    @Builder.Default
    @Column(name = "billing_unit", length = 20)
    private String billingUnit = "piece";

    // ── How many stock units this line consumes ───────────
    // For kg-priced items sold as grams, this is the converted kg value.
    @Builder.Default
    @Column(name = "stock_quantity", precision = 12, scale = 3)
    private BigDecimal stockQuantity = BigDecimal.ZERO;

    // ── Flag for Return/Exchange ──────────────────────────
    @Column(name = "is_return", nullable = false)
    @Builder.Default
    private Boolean isReturn = false;

    // ── GST % applied to this item ────────────────────────
    @Builder.Default
    @Column(name = "gst_percent", precision = 5, scale = 2)
    private BigDecimal gstPercent = BigDecimal.ZERO;

    // ── Calculated total for this line ────────────────────
    // item_total = quantity × unit_price
    // (GST is calculated separately on the Bill level)
    @Builder.Default
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