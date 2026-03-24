package com.example.grocery_billing.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Key-value store for all system settings.
 * Each setting is one row: key=value
 * Example: shop.name = My Grocery Store
 */
@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    @Column(name = "setting_key",
            length = 100, nullable = false)
    private String key;

    @Column(name = "setting_value",
            length = 500)
    private String value;

    @Column(length = 200)
    private String description;
}