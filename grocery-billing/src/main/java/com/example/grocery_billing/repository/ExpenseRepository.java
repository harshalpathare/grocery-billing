package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByShopIdOrderByExpenseDateDescCreatedAtDesc(Long shopId);

    List<Expense> findByShopIdAndExpenseDateBetweenOrderByExpenseDateDesc(Long shopId, LocalDate start, LocalDate end);
    
    List<Expense> findByShopIdAndCategoryAndExpenseDateBetweenOrderByExpenseDateDesc(Long shopId, String category, LocalDate start, LocalDate end);

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.shop.id = :shopId AND e.expenseDate BETWEEN :start AND :end GROUP BY e.category ORDER BY SUM(e.amount) DESC")
    List<Object[]> getCategoryWiseTotal(@Param("shopId") Long shopId, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
