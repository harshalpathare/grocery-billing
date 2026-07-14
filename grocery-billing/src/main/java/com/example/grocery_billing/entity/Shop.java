package com.example.grocery_billing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * SHOP ENTITY
 * Each row = one shop/client using this billing system.
 *
 * SUPER_ADMIN can see all shops.
 * OWNER sees only their shop's data.
 * CASHIER sees only their shop's data (billing only).
 */
@Entity
@Table(name = "shops")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Basic Info ────────────────────────────────────────
    @NotBlank(message = "Shop name is required")
    @Size(max = 150)
    @Column(name = "shop_name", nullable = false, length = 150)
    private String shopName;

    @Size(max = 100)
    @Column(name = "owner_name", length = 100)
    private String ownerName;

    @Size(max = 300)
    @Column(name = "address", length = 300)
    private String address;

    @Size(max = 100)
    @Column(name = "city", length = 100)
    private String city;

    @Size(max = 100)
    @Column(name = "state", length = 100)
    private String state;

    @Size(max = 10)
    @Column(name = "pincode", length = 10)
    private String pincode;

    @Size(max = 15)
    @Column(name = "phone", length = 15)
    private String phone;

    @Email
    @Size(max = 100)
    @Column(name = "email", length = 100)
    private String email;

    // ── GST & Compliance ──────────────────────────────────
    @Size(max = 20)
    @Column(name = "gstin", length = 20)
    private String gstin;

    @Size(max = 20)
    @Column(name = "fssai_no", length = 20)
    private String fssaiNo;

    // ── Payment ───────────────────────────────────────────
    @Size(max = 100)
    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Size(max = 300)
    @Column(name = "bank_details", length = 300)
    private String bankDetails;

    // ── Invoice Customisation ─────────────────────────────
    @Size(max = 300)
    @Column(name = "thank_you_msg", length = 300)
    private String thankYouMsg = "Thank you for shopping with us! Visit again.";

    @Size(max = 500)
    @Column(name = "terms", length = 500)
    private String terms;

    @Size(max = 300)
    @Column(name = "logo_url", length = 300)
    private String logoUrl;

    // ── Business Type ─────────────────────────────────────
    // e.g. GROCERY, MEDICAL, GENERAL, RESTAURANT
    @Size(max = 50)
    @Column(name = "business_type", length = 50)
    @Builder.Default
    private String businessType = "GROCERY";

    // ── Subscription ─────────────────────────────────────
    @Column(name = "subscription_end_date")
    private LocalDate subscriptionEndDate;

    @Column(name = "license_key", unique = true, length = 64)
    private String licenseKey;

    // ── Status ────────────────────────────────────────────
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    // ── Timestamps ────────────────────────────────────────
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Helper ────────────────────────────────────────────
    public boolean isSubscriptionActive() {
        if (subscriptionEndDate == null) return true; // no expiry set
        return !LocalDate.now().isAfter(subscriptionEndDate);
    }
}
