package com.example.grocery_billing.repository;


import com.example.grocery_billing.entity.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface BillItemRepository extends JpaRepository<BillItem, Long> {

    // All items in a specific bill
    List<BillItem> findByBillId(Long billId);

    // Top selling products (for reports dashboard)
    @Query("SELECT bi.product.nameEn, SUM(bi.quantity) as totalQty " +
            "FROM BillItem bi " +
            "WHERE bi.bill.billDate BETWEEN :start AND :end " +
            "GROUP BY bi.product.id, bi.product.nameEn " +
            "ORDER BY totalQty DESC")
    List<Object[]> findTopSellingProducts(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    // ── Inventory Queries (Shop-scoped) ───────────────────
    @Query("SELECT bi.product, SUM(bi.quantity) as totalQty " +
            "FROM BillItem bi JOIN bi.bill b " +
            "WHERE b.shop.id = :shopId AND b.billDate >= :since " +
            "GROUP BY bi.product " +
            "ORDER BY totalQty DESC")
    List<Object[]> findFastMovingProductsByShop(@Param("shopId") Long shopId, @Param("since") LocalDate since);

    @Query("SELECT bi.product, SUM(bi.quantity) as totalQty " +
            "FROM BillItem bi JOIN bi.bill b " +
            "WHERE b.shop.id = :shopId AND b.billDate >= :since " +
            "GROUP BY bi.product " +
            "ORDER BY totalQty ASC")
    List<Object[]> findSlowMovingProductsByShop(@Param("shopId") Long shopId, @Param("since") LocalDate since);

    // ── Inventory Queries (Legacy) ────────────────────────
    @Query("SELECT bi.product, SUM(bi.quantity) as totalQty " +
            "FROM BillItem bi JOIN bi.bill b " +
            "WHERE b.billDate >= :since " +
            "GROUP BY bi.product " +
            "ORDER BY totalQty DESC")
    List<Object[]> findFastMovingProducts(@Param("since") LocalDate since);

    @Query("SELECT bi.product, SUM(bi.quantity) as totalQty " +
            "FROM BillItem bi JOIN bi.bill b " +
            "WHERE b.billDate >= :since " +
            "GROUP BY bi.product " +
            "ORDER BY totalQty ASC")
    List<Object[]> findSlowMovingProducts(@Param("since") LocalDate since);
}