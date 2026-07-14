package com.example.grocery_billing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * USER ENTITY
 *
 * Roles:
 *   ROLE_SUPER_ADMIN → software owner — sees all shops, manages everything
 *   ROLE_OWNER       → shop owner — full access to their own shop
 *   ROLE_CASHIER     → billing staff — can create bills only, no settings/reports
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String password;

    @NotBlank
    @Size(max = 100)
    @Column(name = "full_name", length = 100)
    private String fullName;

    // ── Role ─────────────────────────────────────────────
    // ROLE_SUPER_ADMIN, ROLE_OWNER, ROLE_CASHIER
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String role = "ROLE_CASHIER";

    // ── Shop link ─────────────────────────────────────────
    // NULL for SUPER_ADMIN (they see all shops)
    // Required for OWNER and CASHIER
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    @ToString.Exclude
    private Shop shop;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ── Helper ────────────────────────────────────────────
    public Long getShopId() {
        return shop != null ? shop.getId() : null;
    }

    public boolean isSuperAdmin() {
        return "ROLE_SUPER_ADMIN".equals(role);
    }

    public boolean isOwner() {
        return "ROLE_OWNER".equals(role);
    }

    public boolean isCashier() {
        return "ROLE_CASHIER".equals(role);
    }
}
