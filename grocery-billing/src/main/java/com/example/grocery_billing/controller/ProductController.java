package com.example.grocery_billing.controller;

import com.example.grocery_billing.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Optional;
import com.example.grocery_billing.entity.Product;
import com.example.grocery_billing.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PRODUCT CONTROLLER
 *
 * Handles all HTTP requests for the /products URL path.
 *
 * URL map:
 *   GET  /products          → list all products
 *   GET  /products/new      → show blank add form
 *   POST /products/new      → save new product
 *   GET  /products/{id}/edit → show edit form
 *   POST /products/{id}/edit → save edited product
 *   POST /products/{id}/delete → soft-delete product
 *   GET  /products/search   → JSON search (for billing autocomplete)
 */
@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ─────────────────────────────────────────────────────
    // LIST ALL PRODUCTS
    // GET /products
    // ─────────────────────────────────────────────────────
    @GetMapping
    public String listProducts(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "category", required = false) String category,
            Model model) {

        List<Product> products;

        if (search != null && !search.trim().isEmpty()) {
            products = productService.searchProducts(search);
        } else if (category != null && !category.trim().isEmpty()) {
            products = productService.getProductsByCategory(category);
        } else {
            products = productService.getAllActiveProducts();
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("search", search);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("activePage", "products");
        model.addAttribute("pageTitle", "Products");
        model.addAttribute("totalCount", productService.countActiveProducts());

        return "product/list";
    }

    // ─────────────────────────────────────────────────────
    // SHOW ADD FORM
    // GET /products/new
    // ─────────────────────────────────────────────────────
    @GetMapping("/new")
    public String showAddForm(Model model) {
        // Send a blank Product object — form will fill it
        model.addAttribute("product", new Product());
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("activePage", "products");
        model.addAttribute("pageTitle", "Add Product");
        model.addAttribute("isEdit", false);
        return "product/form";
    }

    // ─────────────────────────────────────────────────────
    // SAVE NEW PRODUCT
    // POST /products/new
    // ─────────────────────────────────────────────────────
    @PostMapping("/new")
    public String saveProduct(
            @Valid @ModelAttribute("product") Product product,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        // If validation errors exist, return to form with error messages
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", productService.getAllCategories());
            model.addAttribute("activePage", "products");
            model.addAttribute("pageTitle", "Add Product");
            model.addAttribute("isEdit", false);
            return "product/form";
        }

        try {
            productService.saveProduct(product);
            // Flash attribute: shows success message on the NEXT page after redirect
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product '" + product.getNameEn() + "' added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error saving product: " + e.getMessage());
        }

        return "redirect:/products";
    }

    // ─────────────────────────────────────────────────────
    // SHOW EDIT FORM
    // GET /products/{id}/edit
    // ─────────────────────────────────────────────────────
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("activePage", "products");
        model.addAttribute("pageTitle", "Edit Product");
        model.addAttribute("isEdit", true);
        return "product/form";
    }

    // ─────────────────────────────────────────────────────
    // SAVE EDITED PRODUCT
    // POST /products/{id}/edit
    // ─────────────────────────────────────────────────────
    @PostMapping("/{id}/edit")
    public String updateProduct(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("product") Product product,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", productService.getAllCategories());
            model.addAttribute("activePage", "products");
            model.addAttribute("pageTitle", "Edit Product");
            model.addAttribute("isEdit", true);
            return "product/form";
        }

        try {
            Product existingProduct = productService.getProductById(id);
            product.setId(id);
            product.setCreatedAt(existingProduct.getCreatedAt());
            
            productService.saveProduct(product);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product '" + product.getNameEn() + "' updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error updating product: " + e.getMessage());
        }

        return "redirect:/products";
    }

    // ─────────────────────────────────────────────────────
    // ── UPDATE STOCK ──────────────────────────────────────
    @PostMapping("/{id}/stock")
    public String updateStock(
            @PathVariable("id") Long id,
            @RequestParam(value = "addQty", required = false) Integer addQty,
            @RequestParam(value = "setQty", required = false) Integer setQty,
            RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.getProductById(id);

            if (setQty != null && setQty >= 0) {
                // Set exact quantity
                product.setStockQty(setQty);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Stock for '" + product.getNameEn()
                                + "' set to " + setQty + ".");
            } else if (addQty != null && addQty > 0) {
                // Add to existing quantity
                int current = product.getStockQty() != null
                        ? product.getStockQty() : 0;
                product.setStockQty(current + addQty);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Added " + addQty + " to '"
                                + product.getNameEn() + "'. New stock: "
                                + product.getStockQty() + ".");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Please enter a valid quantity.");
                return "redirect:/products";
            }

            productService.saveProduct(product);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error updating stock: " + e.getMessage());
        }
        return "redirect:/products";
    }
    // ─────────────────────────────────────────────────────
    // DELETE PRODUCT (soft delete)
    // POST /products/{id}/delete
    // ─────────────────────────────────────────────────────
    @PostMapping("/{id}/delete")
    public String deleteProduct(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product removed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Cannot delete: " + e.getMessage());
        }
        return "redirect:/products";
    }

    // ─────────────────────────────────────────────────────
    // JSON SEARCH API (for billing page autocomplete)
    // GET /products/search?q=sugar&lang=en
    // Returns JSON — used by JavaScript fetch() call
    // ─────────────────────────────────────────────────────
    @GetMapping("/search")
    @ResponseBody  // @ResponseBody = return JSON, not a view name
    public ResponseEntity<List<ProductSearchDto>> searchProducts(
            @RequestParam("q") String q,
            @RequestParam(value = "lang", defaultValue = "en") String lang) {

        List<Product> products = productService.searchForBilling(q);

        List<ProductSearchDto> result = products.stream()
                .map(p -> new ProductSearchDto(
                        p.getId(),
                        p.getNameByLanguage(lang),
                        p.getNameEn(),
                        p.getPrice(),
                        p.getGstPercent(),
                        p.getUnit(),
                        p.getStockQty()
                ))
                .toList();

        return ResponseEntity.ok(result);
    }
    // ─────────────────────────────────────────────────────
// BARCODE LOOKUP API
// ─────────────────────────────────────────────────────
    @GetMapping("/barcode")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> findByBarcode(
            @RequestParam("b") String barcode) {

        Map<String, Object> result = new HashMap<>();
        try {
            Optional<Product> product = productService.findByBarcode(barcode);

            if (product.isPresent()) {
                Product p = product.get();
                result.put("found",    true);
                result.put("id",       p.getId());
                result.put("name",     p.getNameEn());
                result.put("price",    p.getPrice());
                result.put("unit",     p.getUnit());
                result.put("stock",    p.getStockQty());
                result.put("barcode",  p.getBarcode());
            } else {
                result.put("found",   false);
                result.put("message", "Product not found for barcode: " + barcode);
            }
        } catch (Exception e) {
            result.put("found",   false);
            result.put("message", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
    // ─────────────────────────────────────────────────────
    // DTO (Data Transfer Object) for the search API
    // A simple record — just data, no JPA annotations
    // ─────────────────────────────────────────────────────
    public record ProductSearchDto(
            Long id,
            String displayName,
            String nameEn,
            java.math.BigDecimal price,
            java.math.BigDecimal gstPercent,
            String unit,
            Integer stockQty
    ) {}
}

