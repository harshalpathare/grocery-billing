package com.example.grocery_billing.dto.api;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ApiBillItemRequestDto {
    private Long productId;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal gstPercent;
    private Boolean isReturn;
}
