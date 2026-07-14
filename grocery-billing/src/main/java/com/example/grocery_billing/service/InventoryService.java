package com.example.grocery_billing.service;

import com.example.grocery_billing.config.ShopContext;
import com.example.grocery_billing.entity.Product;
import com.example.grocery_billing.repository.BillItemRepository;
import com.example.grocery_billing.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;
    private final BillItemRepository billItemRepository;

    private Long shopId() {
        return ShopContext.getShopId();
    }

    // ─────────────────────────────────────────────────────
    // STOCK REPORTS
    // ─────────────────────────────────────────────────────

    public List<Product> getAllStock() {
        Long sid = shopId();
        if (sid != null) {
            return productRepository.findByShopIdAndActiveTrueOrderByNameEnAsc(sid);
        }
        return productRepository.findByActiveTrueOrderByNameEnAsc();
    }

    public List<Product> getDeadStock(int daysSinceLastSale) {
        LocalDate since = LocalDate.now().minusDays(daysSinceLastSale);
        Long sid = shopId();
        if (sid != null) {
            return productRepository.findDeadStockByShop(sid, since);
        }
        return productRepository.findDeadStock(since);
    }

    public List<Object[]> getFastMovingProducts(int daysBack) {
        LocalDate since = LocalDate.now().minusDays(daysBack);
        Long sid = shopId();
        if (sid != null) {
            return billItemRepository.findFastMovingProductsByShop(sid, since);
        }
        return billItemRepository.findFastMovingProducts(since);
    }

    public List<Object[]> getSlowMovingProducts(int daysBack) {
        LocalDate since = LocalDate.now().minusDays(daysBack);
        Long sid = shopId();
        if (sid != null) {
            return billItemRepository.findSlowMovingProductsByShop(sid, since);
        }
        return billItemRepository.findSlowMovingProducts(since);
    }

    public List<Product> getExpiringProducts(int daysUntilExpiry) {
        LocalDate threshold = LocalDate.now().plusDays(daysUntilExpiry);
        Long sid = shopId();
        if (sid != null) {
            return productRepository.findExpiringProductsByShop(sid, threshold);
        }
        return productRepository.findExpiringProducts(threshold);
    }

    // ─────────────────────────────────────────────────────
    // ALERTS
    // ─────────────────────────────────────────────────────

    public List<Product> getLowStockAlerts() {
        Long sid = shopId();
        if (sid != null) {
            return productRepository.findLowStockItemsByShop(sid);
        }
        return productRepository.findLowStockItems();
    }

    public List<Product> getOutOfStockAlerts() {
        Long sid = shopId();
        if (sid != null) {
            return productRepository.findOutOfStockItemsByShop(sid);
        }
        return productRepository.findOutOfStockItems();
    }

    // ─────────────────────────────────────────────────────
    // DASHBOARD SUMMARY DTO
    // ─────────────────────────────────────────────────────
    
    public record InventorySummary(
            long totalActiveProducts,
            long outOfStockCount,
            long lowStockCount,
            long expiringSoonCount,
            BigDecimal totalStockValue
    ) {}

    public InventorySummary getInventorySummary() {
        List<Product> all = getAllStock();
        long outOfStock = getOutOfStockAlerts().size();
        long lowStock = getLowStockAlerts().size();
        long expiringSoon = getExpiringProducts(30).size();

        BigDecimal stockValue = all.stream()
                .filter(p -> p.getStockQty() != null && p.getStockQty().compareTo(BigDecimal.ZERO) > 0)
                .map(p -> p.getStockQty().multiply(p.getCostPrice() != null ? p.getCostPrice() : (p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InventorySummary(
                all.size(),
                outOfStock,
                lowStock,
                expiringSoon,
                stockValue
        );
    }
}
