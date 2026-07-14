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

    // ── Shop-scoped queries (use these everywhere) ────────

    List<Bill> findByShopIdOrderByCreatedAtDesc(Long shopId);

    Optional<Bill> findByBillNoAndShopId(String billNo, Long shopId);

    List<Bill> findByShopIdAndCustomerIdOrderByBillDateDesc(Long shopId, Long customerId);

    List<Bill> findByShopIdAndBillDateOrderByCreatedAtDesc(Long shopId, LocalDate date);

    List<Bill> findByShopIdAndBillDateBetweenOrderByBillDateDesc(
            Long shopId, LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b " +
           "WHERE b.shop.id = :shopId AND b.billDate = :date")
    BigDecimal getTotalSalesByDateAndShop(
            @Param("shopId") Long shopId,
            @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b " +
           "WHERE b.shop.id = :shopId " +
           "AND YEAR(b.billDate) = :year AND MONTH(b.billDate) = :month")
    BigDecimal getTotalSalesByMonthAndShop(
            @Param("shopId") Long shopId,
            @Param("year") int year,
            @Param("month") int month);

    long countByShopIdAndBillDate(Long shopId, LocalDate date);

    boolean existsByBillNoAndShopId(String billNo, Long shopId);

    @Query("SELECT b.billNo FROM Bill b " +
           "WHERE b.shop.id = :shopId " +
           "AND b.billNo LIKE CONCAT('BILL-', :year, '-%') " +
           "ORDER BY b.id DESC LIMIT 1")
    Optional<String> findLastBillNoForYearAndShop(
            @Param("shopId") Long shopId,
            @Param("year") String year);

    List<Bill> findByShopIdAndIsGstTrueAndBillDateBetween(
            Long shopId, LocalDate start, LocalDate end);

    List<Bill> findByShopIdAndPaymentStatusOrderByBillDateDesc(
            Long shopId, Bill.PaymentStatus status);

    List<Bill> findTop10ByShopIdOrderByCreatedAtDesc(Long shopId);

    // ── Legacy (keep for backward compat during migration) ─
    Optional<Bill> findByBillNo(String billNo);

    List<Bill> findByCustomerIdOrderByBillDateDesc(Long customerId);

    List<Bill> findByBillDateOrderByCreatedAtDesc(LocalDate date);

    List<Bill> findByBillDateBetweenOrderByBillDateDesc(LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b WHERE b.billDate = :date")
    BigDecimal getTotalSalesByDate(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b " +
           "WHERE YEAR(b.billDate) = :year AND MONTH(b.billDate) = :month")
    BigDecimal getTotalSalesByMonth(@Param("year") int year, @Param("month") int month);

    long countByBillDate(LocalDate date);

    boolean existsByBillNo(String billNo);

    @Query("SELECT b.billNo FROM Bill b " +
           "WHERE b.billNo LIKE CONCAT('BILL-', :year, '-%') " +
           "ORDER BY b.id DESC LIMIT 1")
    Optional<String> findLastBillNoForYear(@Param("year") String year);

    @Query("SELECT b.billNo FROM Bill b ORDER BY b.id DESC LIMIT 1")
    Optional<String> findLastBillNo();

    List<Bill> findByIsGstTrueAndBillDateBetween(LocalDate start, LocalDate end);

    List<Bill> findByPaymentStatusOrderByBillDateDesc(Bill.PaymentStatus status);
}
