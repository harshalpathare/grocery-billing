package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.CashFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CashFlowRepository extends JpaRepository<CashFlow, Long> {
    
    List<CashFlow> findByShopIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            Long shopId, LocalDate start, LocalDate end);
            
}
