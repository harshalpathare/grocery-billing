package com.example.grocery_billing.service;

import com.example.grocery_billing.config.ShopContext;
import com.example.grocery_billing.entity.CashFlow;
import com.example.grocery_billing.entity.Shop;
import com.example.grocery_billing.repository.CashFlowRepository;
import com.example.grocery_billing.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashFlowService {

    private final CashFlowRepository cashFlowRepository;
    private final ShopRepository shopRepository;

    public List<CashFlow> getEntriesForDateRange(LocalDate start, LocalDate end) {
        Long shopId = ShopContext.getShopId();
        return cashFlowRepository.findByShopIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(shopId, start, end);
    }

    public CashFlowSummary getSummaryForDateRange(LocalDate start, LocalDate end) {
        List<CashFlow> entries = getEntriesForDateRange(start, end);

        BigDecimal totalIn = entries.stream()
                .filter(e -> e.getType() == CashFlow.TransactionType.IN)
                .map(CashFlow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOut = entries.stream()
                .filter(e -> e.getType() == CashFlow.TransactionType.OUT)
                .map(CashFlow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balance = totalIn.subtract(totalOut);

        return new CashFlowSummary(totalIn, totalOut, balance);
    }

    @Transactional
    public void addEntry(CashFlow entry) {
        Long shopId = ShopContext.getShopId();
        Shop shop = shopRepository.getReferenceById(shopId);
        entry.setShop(shop);
        cashFlowRepository.save(entry);
    }
    
    @Transactional
    public void deleteEntry(Long id) {
        Long shopId = ShopContext.getShopId();
        CashFlow entry = cashFlowRepository.findById(id).orElseThrow();
        if (entry.getShop().getId().equals(shopId)) {
            cashFlowRepository.delete(entry);
        } else {
            throw new RuntimeException("Unauthorized");
        }
    }

    public record CashFlowSummary(BigDecimal totalIn, BigDecimal totalOut, BigDecimal balance) {}
}
