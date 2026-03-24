package com.example.grocery_billing.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(precision = 10, scale = 3)
    private BigDecimal quantity;

    // Cost price per unit from supplier
    @Column(name = "unit_cost", precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "total_cost", precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "product_name_snapshot", length = 200)
    private String productNameSnapshot;

    public void calculateTotal() {
        if (quantity != null && unitCost != null) {
            this.totalCost = quantity.multiply(unitCost)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }
}