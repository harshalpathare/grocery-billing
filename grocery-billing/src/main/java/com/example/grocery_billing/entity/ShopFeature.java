package com.example.grocery_billing.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shop_features", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"shop_id", "feature_name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    @ToString.Exclude
    private Shop shop;

    @Column(name = "feature_name", nullable = false, length = 50)
    private String featureName;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = false;
}
