package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.PurchaseOrder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.query.Param;

public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findBySupplierId(Long supplierId,
                                         Sort sort);

    List<PurchaseOrder> findByOrderDateBetween(
            LocalDate start, LocalDate end);

    List<PurchaseOrder> findByOrderDateBetweenOrderByOrderDateDesc(LocalDate start, LocalDate end);

    @Query("SELECT p.poNumber FROM PurchaseOrder p WHERE p.poNumber LIKE CONCAT('PO-', :year, '-%') ORDER BY p.id DESC LIMIT 1")
    Optional<String> findLastPoForYear(@Param("year") String year);

    boolean existsByPoNumber(String poNumber);

    @Query("SELECT COALESCE(SUM(p.totalAmount),0)" +
            " FROM PurchaseOrder p" +
            " WHERE p.orderDate BETWEEN :start AND :end")
    BigDecimal getTotalPurchasesBetween(LocalDate start,
                                        LocalDate end);
}