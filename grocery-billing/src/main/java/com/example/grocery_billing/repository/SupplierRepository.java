package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    // ── Shop-scoped queries ───────────────────────────────

    List<Supplier> findByShopIdAndActiveTrueOrderByNameAsc(Long shopId);

    List<Supplier> findByShopIdAndNameContainingIgnoreCaseAndActiveTrue(
            Long shopId, String name);

    @Query("SELECT COALESCE(SUM(s.balance), 0) FROM Supplier s " +
           "WHERE s.shop.id = :shopId AND s.active = true AND s.balance > 0")
    BigDecimal getTotalPayableByShop(@Param("shopId") Long shopId);

    // ── Legacy ────────────────────────────────────────────
    List<Supplier> findByActiveTrueOrderByNameAsc();

    List<Supplier> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    @Query("SELECT COALESCE(SUM(s.balance), 0) FROM Supplier s " +
           "WHERE s.active = true AND s.balance > 0")
    BigDecimal getTotalPayable();
    @Query("SELECT s FROM Supplier s WHERE s.shop.id = :shopId AND s.active = true AND s.totalPayable > coalesce(s.totalPaid, 0) ORDER BY s.name ASC")
    List<Supplier> findSuppliersWithPendingBalance(@Param("shopId") Long shopId);

    @Query("SELECT s FROM Supplier s WHERE s.active = true AND s.totalPayable > coalesce(s.totalPaid, 0) ORDER BY s.name ASC")
    List<Supplier> findAllSuppliersWithPendingBalance();
}
