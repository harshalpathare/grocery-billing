package com.example.grocery_billing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * USER ENTITY
 * Stores login credentials for shop staff.
 *
 * Passwords are stored as BCrypt hashes — NEVER plain text.
 * Example: password "admin123" → "$2a$10$xyz..."
 *
 * Roles:
 *   ROLE_ADMIN → full access (add/edit/delete everything)
 *   ROLE_USER  → view + billing only (cannot delete or manage settings)
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

    // Stores BCrypt hash — minimum 60 chars
    @NotBlank
    @Column(nullable = false, length = 100)
    private String password;

    @NotBlank
    @Size(max = 100)
    @Column(name = "full_name", length = 100)
    private String fullName;

    // ROLE_ADMIN or ROLE_USER
    @Column(nullable = false, length = 20)
    private String role = "ROLE_USER";

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}