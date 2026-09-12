package com.example.grocery_billing.dto.api;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ApiProductResponseDto {
    private Long id;
    private String nameEn;
    private String nameHi;
    private String nameMr;
    private String sku;
    private BigDecimal salesPrice;
    private BigDecimal stock;
    private String unit;
}
