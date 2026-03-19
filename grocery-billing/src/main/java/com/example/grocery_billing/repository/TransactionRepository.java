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
            Long customerId);

    List<Transaction> findByTransactionDateBetweenOrderByTransactionDateDesc(
            LocalDate start, LocalDate end);

    List<Transaction> findByTypeOrderByTransactionDateDesc(
            Transaction.TransactionType type);

    List<Transaction> findByCustomerIdAndTypeOrderByTransactionDateDesc(
            Long customerId, Transaction.TransactionType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.type = 'DEBIT' AND t.transactionDate BETWEEN :start AND :end")
    java.math.BigDecimal getTotalPaymentsCollected(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    // ✅ ADD THIS — finds all transactions linked to a specific bill
    List<Transaction> findByBillId(Long billId);
}