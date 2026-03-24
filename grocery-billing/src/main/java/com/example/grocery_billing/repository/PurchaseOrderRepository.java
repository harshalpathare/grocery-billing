package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.PurchaseOrder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findBySupplierId(Long supplierId,
                                         Sort sort);

    List<PurchaseOrder> findByOrderDateBetween(
            LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(MAX(p.poNumber),'')" +
            " FROM PurchaseOrder p WHERE p.poNumber" +
            " LIKE CONCAT('PO-',:year,'-%')")
    Optional<String> findLastPoForYear(String year);

    boolean existsByPoNumber(String poNumber);

    @Query("SELECT COALESCE(SUM(p.totalAmount),0)" +
            " FROM PurchaseOrder p" +
            " WHERE p.orderDate BETWEEN :start AND :end")
    BigDecimal getTotalPurchasesBetween(LocalDate start,
                                        LocalDate end);
}