package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByBillNo(@org.springframework.data.repository.query.Param("billNo") String billNo);

    List<Bill> findByCustomerIdOrderByBillDateDesc(@org.springframework.data.repository.query.Param("customerId") Long customerId);

    List<Bill> findByBillDateOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("date") LocalDate date);

    List<Bill> findByBillDateBetweenOrderByBillDateDesc(@org.springframework.data.repository.query.Param("start") LocalDate start, @org.springframework.data.repository.query.Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b WHERE b.billDate = :date")
    BigDecimal getTotalSalesByDate(@org.springframework.data.repository.query.Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b " +
            "WHERE YEAR(b.billDate) = :year AND MONTH(b.billDate) = :month")
    BigDecimal getTotalSalesByMonth(@org.springframework.data.repository.query.Param("year") int year, @org.springframework.data.repository.query.Param("month") int month);

    long countByBillDate(@org.springframework.data.repository.query.Param("date") LocalDate date);

    // ✅ NEW: Check existence by bill number
    boolean existsByBillNo(@org.springframework.data.repository.query.Param("billNo") String billNo);

    // ✅ NEW: Find last bill number for a specific year
    @Query("SELECT b.billNo FROM Bill b " +
            "WHERE b.billNo LIKE CONCAT('BILL-', :year, '-%') " +
            "ORDER BY b.id DESC LIMIT 1")
    Optional<String> findLastBillNoForYear(@org.springframework.data.repository.query.Param("year") String year);

    // Keep old method for backward compatibility
    @Query("SELECT b.billNo FROM Bill b ORDER BY b.id DESC LIMIT 1")
    Optional<String> findLastBillNo();

    List<Bill> findByIsGstTrueAndBillDateBetween(@org.springframework.data.repository.query.Param("start") LocalDate start, @org.springframework.data.repository.query.Param("end") LocalDate end);

    List<Bill> findByPaymentStatusOrderByBillDateDesc(Bill.PaymentStatus status);
}

