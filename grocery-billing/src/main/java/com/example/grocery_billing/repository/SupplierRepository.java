package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface SupplierRepository
        extends JpaRepository<Supplier, Long> {

    List<Supplier> findByActiveTrueOrderByNameAsc();

    List<Supplier> findByNameContainingIgnoreCaseAndActiveTrue(
            @org.springframework.data.repository.query.Param("name") String name);

    @Query("SELECT COALESCE(SUM(s.balance),0) " +
            "FROM Supplier s WHERE s.active=true " +
            "AND s.balance > 0")
    BigDecimal getTotalPayable();
}
