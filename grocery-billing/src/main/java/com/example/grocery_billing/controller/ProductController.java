package com.example.grocery_billing.controller;


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

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
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
            String displayName = product.getNameEn() != null && !product.getNameEn().isBlank()
                    ? product.getNameEn()
                    : (product.getNameHi() != null && !product.getNameHi().isBlank()
                        ? product.getNameHi() : product.getNameMr());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Product '" + displayName + "' added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error saving product: " + e.getMessage());
        }

        return "redirect:/products";
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createProductInline(
            @RequestParam String nameEn,
            @RequestParam(required = false) String nameHi,
            @RequestParam(required = false) String nameMr,
            @RequestParam(required = false) String hsnCode,
            @RequestParam BigDecimal price,
            @RequestParam(defaultValue = "0") BigDecimal gstPercent,
            @RequestParam(defaultValue = "piece") String unit,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") BigDecimal stockQty) {

        if ((nameEn == null || nameEn.trim().isEmpty())
                && (nameHi == null || nameHi.trim().isEmpty())
                && (nameMr == null || nameMr.trim().isEmpty())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Please enter at least one product name (English, Hindi or Marathi)"));
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Price must be greater than 0"));
        }

        Product product = new Product();
        product.setNameEn(nameEn.trim());
        product.setNameHi(nameHi != null ? nameHi.trim() : null);
        product.setNameMr(nameMr != null ? nameMr.trim() : null);
        product.setHsnCode(hsnCode != null ? hsnCode.trim() : null);
        product.setPrice(price);
        product.setGstPercent(gstPercent != null ? gstPercent : BigDecimal.ZERO);
        product.setUnit(unit != null && !unit.isBlank() ? unit.trim() : "piece");
        product.setCategory(category != null && !category.isBlank() ? category.trim() : null);
        product.setStockQty(stockQty != null ? stockQty : BigDecimal.ZERO);
        product.setActive(true);

        productService.saveProduct(product);

        // Use whichever name was provided as display name
        String displayName = (nameEn != null && !nameEn.isBlank()) ? nameEn.trim()
                           : (nameHi != null && !nameHi.isBlank()) ? nameHi.trim()
                           : nameMr.trim();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Product created successfully");
        response.put("id", product.getId());
        response.put("displayName", displayName);
        response.put("price", product.getPrice());
        response.put("gstPercent", product.getGstPercent());
        response.put("unit", product.getUnit());
        response.put("category", product.getCategory());
        response.put("stockQty", product.getStockQty());
        response.put("hsnCode", product.getHsnCode());
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────
    // SHOW EDIT FORM
    // GET /products/{id}/edit
    // ─────────────────────────────────────────────────────
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("activePage", "products");
        model.addAttribute("pageTitle", "Edit Product");
        model.addAttribute("isEdit", true);
        return "product/form";
    }

    @PostMapping("/{id}/edit")
    public String updateProduct(
            @PathVariable Long id,
            @Valid @ModelAttribute("product") Product formProduct,
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
            Product product = productService.getProductById(id);
            product.setNameEn(formProduct.getNameEn());
            product.setNameHi(formProduct.getNameHi());
            product.setNameMr(formProduct.getNameMr());
            product.setHsnCode(formProduct.getHsnCode());
            product.setPrice(formProduct.getPrice());
            product.setGstPercent(formProduct.getGstPercent());
            product.setUnit(formProduct.getUnit());
            product.setStockQty(formProduct.getStockQty());
            product.setCategory(formProduct.getCategory());
            product.setActive(formProduct.getActive() != null ? formProduct.getActive() : Boolean.TRUE);

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
    // SAVE EDITED PRODUCT
    // POST /products/{id}/edit
    // ─────────────────────────────────────────────────────
    // ── UPDATE STOCK ──────────────────────────────────────
    @PostMapping("/{id}/stock")
    public String updateStock(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal addQty,
            @RequestParam(required = false) BigDecimal setQty,
            RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.getProductById(id);

            if (setQty != null && setQty.compareTo(BigDecimal.ZERO) >= 0) {
                // Set exact quantity
                product.setStockQty(setQty);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Stock for '" + product.getNameEn()
                                + "' set to " + setQty.stripTrailingZeros().toPlainString() + ".");
            } else if (addQty != null && addQty.compareTo(BigDecimal.ZERO) > 0) {
                // Add to existing quantity
                BigDecimal current = product.getStockQty() != null
                        ? product.getStockQty() : BigDecimal.ZERO;
                product.setStockQty(current.add(addQty));
                redirectAttributes.addFlashAttribute("successMessage",
                        "Added " + addQty.stripTrailingZeros().toPlainString() + " to '"
                                + product.getNameEn() + "'. New stock: "
                                + product.getStockQty().stripTrailingZeros().toPlainString() + ".");
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
            @PathVariable Long id,
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
    @RequestMapping(value = "/search", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody  // @ResponseBody = return JSON, not a view name
    public ResponseEntity<List<ProductSearchDto>> searchProducts(
            @RequestParam String q,
            @RequestParam(defaultValue = "en") String lang) {

        List<Product> products = productService.searchForBilling(q);

        List<ProductSearchDto> result = products.stream()
                .map(p -> new ProductSearchDto(
                        p.getId(),
                        p.getNameByLanguage(lang),
                        p.getNameEn(),
                        p.getPrice(),
                        p.getGstPercent(),
                        p.getUnit(),
                    p.getCategory(),
                        p.getStockQty()
                ))
                .toList();

        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = "/category", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public ResponseEntity<List<ProductSearchDto>> productsByCategory(
            @RequestParam String category,
            @RequestParam(defaultValue = "en") String lang) {

        List<Product> products = productService.getProductsByCategory(category);

        List<ProductSearchDto> result = products.stream()
                .map(p -> new ProductSearchDto(
                        p.getId(),
                        p.getNameByLanguage(lang),
                        p.getNameEn(),
                        p.getPrice(),
                        p.getGstPercent(),
                        p.getUnit(),
                        p.getCategory(),
                        p.getStockQty()
                ))
                .toList();

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
            String category,
            java.math.BigDecimal stockQty
    ) {}
}