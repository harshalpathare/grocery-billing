package com.example.grocery_billing.dto.api;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ApiCustomerResponseDto {
    private Long id;
    private String name;
    private String phone;
    private BigDecimal balance;
}
