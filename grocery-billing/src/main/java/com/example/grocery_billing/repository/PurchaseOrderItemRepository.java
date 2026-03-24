package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.PurchaseOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseOrderItemRepository
        extends JpaRepository<PurchaseOrderItem, Long> {

    List<PurchaseOrderItem> findByPurchaseOrderId(Long poId);

    List<PurchaseOrderItem> findByProductId(Long productId);
}