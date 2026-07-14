package com.example.grocery_billing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PRODUCT ENTITY
 * Maps to the 'products' table in MySQL.
 *
 * Real-world example:
 *   Sugar → name_en="Sugar", name_hi="चीनी", name_mr="साखर"
 *   price=45.00, gst_percent=5.0, stock_qty=100
 */
@Entity
@Table(name = "products")
@Data                    // Lombok: auto-generates getters, setters, toString
@NoArgsConstructor       // Lombok: generates empty constructor
@AllArgsConstructor      // Lombok: generates constructor with all fields
@Builder                 // Lombok: lets you do Product.builder().name("...").build()
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Shop (multi-tenant) ───────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    @ToString.Exclude
    private Shop shop;

    // ── English name ──────────────────────────────────────
    // At least one of nameEn, nameHi, nameMr must be provided
    @Size(max = 100)
    @Column(name = "name_en", nullable = true, length = 100)
    private String nameEn;

    // ── Hindi name ───────────────────────────────────────
    // columnDefinition = "VARCHAR(100) CHARACTER SET utf8mb4"
    // This ensures Hindi characters are stored correctly
    @Size(max = 100)
    @Column(name = "name_hi", length = 100,
            columnDefinition = "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String nameHi;

    // ── Marathi name ─────────────────────────────────────
    @Size(max = 100)
    @Column(name = "name_mr", length = 100,
            columnDefinition = "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
    private String nameMr;

    // Add this field to Product entity
    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    // ✅ ADD THIS
    @Column(name = "hsn_code", length = 20)
    private String hsnCode;
    // ── Price ────────────────────────────────────────────
    // BigDecimal is used for money — never use double/float for prices!
    // precision=10 means max 10 digits total
    // scale=2 means 2 decimal places → e.g. 999.99
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // ── GST Percentage ───────────────────────────────────
    // Common GST rates: 0%, 5%, 12%, 18%, 28%
    @Builder.Default
    @DecimalMin("0.0")
    @DecimalMax("28.0")
    @Column(name = "gst_percent", precision = 5, scale = 2)
    private BigDecimal gstPercent = BigDecimal.ZERO;

    // ── Stock Quantity ───────────────────────────────────
    @DecimalMin(value = "0.0", inclusive = true, message = "Stock cannot be negative")
    @Column(name = "stock_qty")
    @Builder.Default
    private BigDecimal stockQty = BigDecimal.ZERO;

    // ── Unit of measurement ──────────────────────────────
    // e.g. "kg", "litre", "piece", "dozen"
    @Size(max = 20)
    @Builder.Default
    @Column(name = "unit", length = 20)
    private String unit = "piece";

    // ── Category ─────────────────────────────────────────
    @Size(max = 50)
    @Column(length = 50)
    private String category;

    // ── Active flag ──────────────────────────────────────
    // false = deleted/hidden product (soft delete)
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    // ── NEW FIELDS ───────────────────────────────────────
    @Size(max = 100)
    @Column(name = "barcode", length = 100)
    private String barcode;

    @Size(max = 100)
    @Column(name = "sku", length = 100)
    private String sku;

    @Size(max = 100)
    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "mrp", precision = 10, scale = 2)
    private BigDecimal mrp;

    @DecimalMin(value = "0.0")
    @Column(name = "min_stock")
    @Builder.Default
    private BigDecimal minStock = BigDecimal.ZERO;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Size(max = 100)
    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Size(max = 255)
    @Column(name = "image_url", length = 255)
    private String imageUrl;

    // ── Timestamps ───────────────────────────────────────
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Auto-set timestamps before saving
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Helper method ────────────────────────────────────
    /**
     * Returns the product name based on selected language.
     * Usage: product.getNameByLanguage("hi") → returns Hindi name
     */
    public String getNameByLanguage(String lang) {
        if ("hi".equals(lang) && nameHi != null && !nameHi.isBlank()) {
            return nameHi;
        } else if ("mr".equals(lang) && nameMr != null && !nameMr.isBlank()) {
            return nameMr;
        }
        // Fallback: return whichever name is available
        if (nameEn != null && !nameEn.isBlank()) return nameEn;
        if (nameHi != null && !nameHi.isBlank()) return nameHi;
        if (nameMr != null && !nameMr.isBlank()) return nameMr;
        return "";
    }
}