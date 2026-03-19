package com.example.grocery_billing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
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

    // ── English name (required) ──────────────────────────
    @NotBlank(message = "English name is required")
    @Size(max = 100)
    @Column(name = "name_en", nullable = false, length = 100)
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
    @DecimalMin("0.0")
    @DecimalMax("28.0")
    @Column(name = "gst_percent", precision = 5, scale = 2)
    private BigDecimal gstPercent = BigDecimal.ZERO;

    // ── Stock Quantity ───────────────────────────────────
    @Min(value = 0, message = "Stock cannot be negative")
    @Column(name = "stock_qty")
    private Integer stockQty = 0;

    // ── Unit of measurement ──────────────────────────────
    // e.g. "kg", "litre", "piece", "dozen"
    @Size(max = 20)
    @Column(name = "unit", length = 20)
    private String unit = "piece";

    // ── Category ─────────────────────────────────────────
    @Size(max = 50)
    @Column(length = 50)
    private String category;

    // ── Active flag ──────────────────────────────────────
    // false = deleted/hidden product (soft delete)
    @Column(nullable = false)
    private Boolean active = true;

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
        return nameEn; // default to English
    }
}