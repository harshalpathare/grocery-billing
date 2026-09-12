package com.example.grocery_billing.controller.api;

import com.example.grocery_billing.dto.api.ApiProductResponseDto;
import com.example.grocery_billing.entity.Product;
import com.example.grocery_billing.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ApiProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ApiProductResponseDto>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        List<ApiProductResponseDto> response = products.stream().map(p -> ApiProductResponseDto.builder()
                .id(p.getId())
                .nameEn(p.getNameEn())
                .nameHi(p.getNameHi())
                .nameMr(p.getNameMr())
                .sku(p.getSku())
                .salesPrice(p.getPrice())
                .stock(p.getStockQty())
                .unit(p.getUnit())
                .build()
        ).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiProductResponseDto> getProductById(@PathVariable Long id) {
        Product p = productService.getProductById(id);
        ApiProductResponseDto response = ApiProductResponseDto.builder()
                .id(p.getId())
                .nameEn(p.getNameEn())
                .nameHi(p.getNameHi())
                .nameMr(p.getNameMr())
                .sku(p.getSku())
                .salesPrice(p.getPrice())
                .stock(p.getStockQty())
                .unit(p.getUnit())
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiProductResponseDto> createProduct(@RequestBody com.example.grocery_billing.dto.api.ApiProductRequestDto request) {
        Product product = new Product();
        product.setNameEn(request.getNameEn());
        product.setNameHi(request.getNameHi());
        product.setNameMr(request.getNameMr());
        product.setSku(request.getSku());
        product.setPrice(request.getSalesPrice());
        if (request.getCostPrice() != null) {
            product.setCostPrice(request.getCostPrice());
        }
        product.setStockQty(request.getStock());
        product.setUnit(request.getUnit());
        product.setGstPercent(request.getGstPercent() != null ? request.getGstPercent() : java.math.BigDecimal.ZERO);
        product.setBarcode(request.getBarcode());
        product.setCategory(request.getCategory());
        
        Product saved = productService.saveProduct(product);
        
        ApiProductResponseDto response = ApiProductResponseDto.builder()
                .id(saved.getId())
                .nameEn(saved.getNameEn())
                .nameHi(saved.getNameHi())
                .nameMr(saved.getNameMr())
                .sku(saved.getSku())
                .salesPrice(saved.getPrice())
                .stock(saved.getStockQty())
                .unit(saved.getUnit())
                .build();
                
        return ResponseEntity.ok(response);
    }
}
