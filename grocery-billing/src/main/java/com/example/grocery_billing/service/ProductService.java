package com.example.grocery_billing.service;


import com.example.grocery_billing.entity.Product;
import com.example.grocery_billing.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * PRODUCT SERVICE
 *
 * This is the "brain" of the product module.
 * Controller calls Service. Service calls Repository.
 * Never put business logic in Controller or Repository.
 *
 * @Service   = marks this as a Spring-managed service bean
 * @RequiredArgsConstructor = Lombok: auto-injects ProductRepository via constructor
 * @Transactional = wraps DB operations in a transaction (auto-rollback on error)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    // ─────────────────────────────────────────────────────
    // GET ALL PRODUCTS
    // ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameEnAsc();
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // ─────────────────────────────────────────────────────
    // GET SINGLE PRODUCT
    // ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    // ─────────────────────────────────────────────────────
    // SEARCH PRODUCTS (used in billing page search box)
    // ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllActiveProducts();
        }
        return productRepository.searchByAllLanguages(keyword.trim());
    }

    // ─────────────────────────────────────────────────────
    // SEARCH FOR BILLING API (returns limited fields for speed)
    // ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Product> searchForBilling(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        return productRepository.searchByAllLanguages(keyword.trim());
    }

    // ─────────────────────────────────────────────────────
    // SAVE (both create and update)
    // ─────────────────────────────────────────────────────
    public Product saveProduct(Product product) {
        // Trim all text fields
        if (product.getNameEn() != null) {
            product.setNameEn(product.getNameEn().trim());
        }
        if (product.getNameHi() != null) {
            product.setNameHi(product.getNameHi().trim());
        }
        if (product.getNameMr() != null) {
            product.setNameMr(product.getNameMr().trim());
        }
        return productRepository.save(product);
    }

    // ─────────────────────────────────────────────────────
    // SOFT DELETE (don't actually delete — just mark inactive)
    // Why? Old bills still reference this product.
    // Hard delete would break those bills.
    // ─────────────────────────────────────────────────────
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    // ─────────────────────────────────────────────────────
    // RESTORE a soft-deleted product
    // ─────────────────────────────────────────────────────
    public void restoreProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(true);
        productRepository.save(product);
    }

    // ─────────────────────────────────────────────────────
    // COUNT (for dashboard stats)
    // ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public long countActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameEnAsc().size();
    }

    // ─────────────────────────────────────────────────────
    // GET ALL CATEGORIES (for filter dropdown)
    // ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    // ─────────────────────────────────────────────────────
    // GET BY CATEGORY
    // ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndActiveTrue(category);
    }
}