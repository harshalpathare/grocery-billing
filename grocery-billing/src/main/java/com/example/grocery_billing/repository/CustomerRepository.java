package com.example.grocery_billing.repository;


import com.example.grocery_billing.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByPhone(String phone);

    // ✅ NEW — find soft-deleted customer by phone
    Optional<Customer> findByPhoneAndActiveFalse(String phone);

    // ✅ Check phone among ACTIVE customers only
    boolean existsByPhoneAndActiveTrue(String phone);

    // ✅ Check phone among ACTIVE customers, excluding current customer (for edit)
    boolean existsByPhoneAndIdNotAndActiveTrue(String phone, Long id);

    List<Customer> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    List<Customer> findByActiveTrueOrderByNameAsc();

    List<Customer> findByBalanceGreaterThanAndActiveTrue(BigDecimal amount);



    // ✅ NEW — only sum positive balances
    @Query("SELECT COALESCE(SUM(c.balance), 0) FROM Customer c " +
            "WHERE c.active = true AND c.balance > 0")
    BigDecimal getTotalPendingBalance();

}