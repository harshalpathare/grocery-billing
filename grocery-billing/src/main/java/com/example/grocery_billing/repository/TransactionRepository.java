package com.example.grocery_billing.repository;



import com.example.grocery_billing.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByCustomerIdOrderByTransactionDateDescCreatedAtDesc(
            @org.springframework.data.repository.query.Param("customerId") Long customerId);

    List<Transaction> findByTransactionDateBetweenOrderByTransactionDateDesc(
            @org.springframework.data.repository.query.Param("start") LocalDate start, @org.springframework.data.repository.query.Param("end") LocalDate end);

    List<Transaction> findByTypeOrderByTransactionDateDesc(
            @org.springframework.data.repository.query.Param("type") Transaction.TransactionType type);

    List<Transaction> findByCustomerIdAndTypeOrderByTransactionDateDesc(
            @org.springframework.data.repository.query.Param("customerId") Long customerId, @org.springframework.data.repository.query.Param("type") Transaction.TransactionType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.type = 'DEBIT' AND t.transactionDate BETWEEN :start AND :end")
    java.math.BigDecimal getTotalPaymentsCollected(
            @org.springframework.data.repository.query.Param("start") LocalDate start,
            @org.springframework.data.repository.query.Param("end") LocalDate end);

    // ✅ ADD THIS — finds all transactions linked to a specific bill
    List<Transaction> findByBillId(@org.springframework.data.repository.query.Param("billId") Long billId);
}
