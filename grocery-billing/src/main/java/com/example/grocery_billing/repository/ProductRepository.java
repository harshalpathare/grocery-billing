package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find all active products (not soft-deleted)
    List<Product> findByActiveTrueOrderByNameEnAsc();

    // Search products by English name (case-insensitive)
    // Spring auto-generates SQL: WHERE name_en LIKE %keyword%
    List<Product> findByNameEnContainingIgnoreCaseAndActiveTrue(String keyword);

    // Find by category
    List<Product> findByCategoryAndActiveTrue(String category);

    // Custom query: search across all 3 languages at once
    // Used for the billing page search box
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
            "(LOWER(p.nameEn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(p.nameHi) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(p.nameMr) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchByAllLanguages(@Param("keyword") String keyword);

    // Check if a product name already exists (prevent duplicates)
    Optional<Product> findByNameEnIgnoreCase(String nameEn);

    // Get all distinct categories
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL AND p.active = true")
    List<String> findAllCategories();
}