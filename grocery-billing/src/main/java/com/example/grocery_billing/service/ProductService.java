package com.example.grocery_billing.service;

import com.example.grocery_billing.config.ShopContext;
import com.example.grocery_billing.entity.Product;
import com.example.grocery_billing.entity.Shop;
import com.example.grocery_billing.repository.ProductRepository;
import com.example.grocery_billing.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository    shopRepository;

    // ── helpers ───────────────────────────────────────────
    private Long shopId() {
        return ShopContext.getShopId();
    }

    private Shop currentShop() {
        Long id = shopId();
        if (id == null) throw new RuntimeException("No shop in context");
        return shopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop not found: " + id));
    }

    // ─────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Product> getAllActiveProducts() {
        Long id = shopId();
        if (id != null) return productRepository.findByShopIdAndActiveTrueOrderByNameEnAsc(id);
        return productRepository.findByActiveTrueOrderByNameEnAsc();
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return getAllActiveProducts();
        Long id = shopId();
        if (id != null) return productRepository.searchByShopAndAllLanguages(id, keyword.trim());
        return productRepository.searchByAllLanguages(keyword.trim());
    }

    @Transactional(readOnly = true)
    public List<Product> searchForBilling(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return List.of();
        Long id = shopId();
        if (id != null) return productRepository.searchByShopAndAllLanguages(id, keyword.trim());
        return productRepository.searchByAllLanguages(keyword.trim());
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        Long id = shopId();
        List<String> list = (id != null) ? productRepository.findAllCategoriesByShop(id) : productRepository.findAllCategories();
        return list.stream()
                   .filter(c -> c != null && !c.trim().isEmpty())
                   .toList();
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByCategory(String category) {
        Long id = shopId();
        if (id != null) return productRepository.findByShopIdAndCategoryAndActiveTrue(id, category);
        return productRepository.findByCategoryAndActiveTrue(category);
    }

    @Transactional(readOnly = true)
    public long countActiveProducts() {
        Long id = shopId();
        if (id != null) return productRepository.countByShopIdAndActiveTrue(id);
        return productRepository.findByActiveTrueOrderByNameEnAsc().size();
    }

    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts(BigDecimal threshold) {
        Long id = shopId();
        if (id != null) return productRepository.findLowStockByShop(id, threshold);
        return List.of();
    }

    // ─────────────────────────────────────────────────────
    // WRITE
    // ─────────────────────────────────────────────────────

    public Product saveProduct(Product product) {
        // Attach shop if not already set
        if (product.getShop() == null) {
            product.setShop(currentShop());
        }
        if (product.getNameEn() != null) product.setNameEn(product.getNameEn().trim());
        if (product.getNameHi() != null) product.setNameHi(product.getNameHi().trim());
        if (product.getNameMr() != null) product.setNameMr(product.getNameMr().trim());
        return productRepository.save(product);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_OWNER', 'ROLE_SUPER_ADMIN')")
    public void restoreProduct(Long id) {
        Product product = getProductById(id);
        product.setActive(true);
        productRepository.save(product);
    }
}
