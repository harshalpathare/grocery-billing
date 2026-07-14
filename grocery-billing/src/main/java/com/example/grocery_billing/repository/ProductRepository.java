package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // ── Shop-scoped queries ───────────────────────────────

    List<Product> findByShopIdAndActiveTrueOrderByNameEnAsc(Long shopId);

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND p.active = true AND " +
           "(LOWER(p.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.nameHi) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.nameMr) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchByShopAndAllLanguages(
            @Param("shopId") Long shopId,
            @Param("keyword") String keyword);

    List<Product> findByShopIdAndCategoryAndActiveTrue(Long shopId, String category);

    @Query("SELECT DISTINCT p.category FROM Product p " +
           "WHERE p.shop.id = :shopId AND p.category IS NOT NULL AND p.active = true")
    List<String> findAllCategoriesByShop(@Param("shopId") Long shopId);

    long countByShopIdAndActiveTrue(Long shopId);

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId " +
           "AND p.active = true AND p.stockQty <= :threshold")
    List<Product> findLowStockByShop(
            @Param("shopId") Long shopId,
            @Param("threshold") java.math.BigDecimal threshold);

    Optional<Product> findByShopIdAndBarcode(Long shopId, String barcode);
    Optional<Product> findByShopIdAndSku(Long shopId, String sku);
    Optional<Product> findByShopIdAndNameEn(Long shopId, String nameEn);

    // ── Inventory Queries (Shop-scoped) ───────────────────
    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND p.active = true AND p.stockQty > 0 AND p.id NOT IN (SELECT bi.product.id FROM BillItem bi JOIN bi.bill b WHERE b.shop.id = :shopId AND b.billDate >= :since)")
    List<Product> findDeadStockByShop(@Param("shopId") Long shopId, @Param("since") LocalDate since);

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND p.active = true AND p.expiryDate IS NOT NULL AND p.expiryDate <= :threshold ORDER BY p.expiryDate ASC")
    List<Product> findExpiringProductsByShop(@Param("shopId") Long shopId, @Param("threshold") LocalDate threshold);

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND p.active = true AND p.stockQty > 0 AND p.stockQty <= p.minStock")
    List<Product> findLowStockItemsByShop(@Param("shopId") Long shopId);

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId AND p.active = true AND p.stockQty <= 0")
    List<Product> findOutOfStockItemsByShop(@Param("shopId") Long shopId);

    // ── Legacy (keep for backward compat) ─────────────────
    List<Product> findByActiveTrueOrderByNameEnAsc();

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.nameHi) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.nameMr) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchByAllLanguages(@Param("keyword") String keyword);

    List<Product> findByCategoryAndActiveTrue(String category);

    Optional<Product> findByNameEnIgnoreCase(String nameEn);

    @Query("SELECT DISTINCT p.category FROM Product p " +
           "WHERE p.category IS NOT NULL AND p.active = true")
    List<String> findAllCategories();

    // ── Inventory Queries (Legacy) ────────────────────────
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stockQty > 0 AND p.id NOT IN (SELECT bi.product.id FROM BillItem bi JOIN bi.bill b WHERE b.billDate >= :since)")
    List<Product> findDeadStock(@Param("since") LocalDate since);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.expiryDate IS NOT NULL AND p.expiryDate <= :threshold ORDER BY p.expiryDate ASC")
    List<Product> findExpiringProducts(@Param("threshold") LocalDate threshold);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stockQty > 0 AND p.stockQty <= p.minStock")
    List<Product> findLowStockItems();

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stockQty <= 0")
    List<Product> findOutOfStockItems();
}
