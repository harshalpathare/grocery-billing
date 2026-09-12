package com.example.grocery_billing.dto.api;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApiProductRequestDto {
    private String nameEn;
    private String nameHi;
    private String nameMr;
    private String sku;
    private BigDecimal costPrice;
    private BigDecimal salesPrice;
    private BigDecimal stock;
    private String unit;
    private BigDecimal gstPercent;
    private String barcode;
    private String category;
}
