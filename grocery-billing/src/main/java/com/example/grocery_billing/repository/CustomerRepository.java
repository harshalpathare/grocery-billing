package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // ── Shop-scoped queries ───────────────────────────────

    List<Customer> findByShopIdAndActiveTrueOrderByNameAsc(Long shopId);

    Optional<Customer> findByPhoneAndShopId(String phone, Long shopId);

    boolean existsByPhoneAndShopIdAndActiveTrue(String phone, Long shopId);

    boolean existsByPhoneAndShopIdAndIdNotAndActiveTrue(
            String phone, Long shopId, Long id);

    List<Customer> findByShopIdAndNameContainingIgnoreCaseAndActiveTrue(
            Long shopId, String name);

    List<Customer> findByShopIdAndBalanceGreaterThanAndActiveTrue(
            Long shopId, BigDecimal amount);

    long countByShopIdAndActiveTrue(Long shopId);

    @Query("SELECT COALESCE(SUM(c.balance), 0) FROM Customer c " +
           "WHERE c.shop.id = :shopId AND c.active = true AND c.balance > 0")
    BigDecimal getTotalPendingBalanceByShop(@Param("shopId") Long shopId);

    // ── Legacy ────────────────────────────────────────────
    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByPhoneAndActiveFalse(String phone);

    boolean existsByPhoneAndActiveTrue(String phone);

    boolean existsByPhoneAndIdNotAndActiveTrue(String phone, Long id);

    List<Customer> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    List<Customer> findByActiveTrueOrderByNameAsc();

    List<Customer> findByBalanceGreaterThanAndActiveTrue(BigDecimal amount);

    @Query("SELECT COALESCE(SUM(c.balance), 0) FROM Customer c " +
           "WHERE c.active = true AND c.balance > 0")
    BigDecimal getTotalPendingBalance();
}
