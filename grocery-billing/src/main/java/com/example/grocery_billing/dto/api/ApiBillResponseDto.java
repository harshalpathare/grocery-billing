package com.example.grocery_billing.dto.api;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ApiBillResponseDto {
    private Long id;
    private String billNo;
    private LocalDate billDate;
    private String customerName;
    private BigDecimal subTotal;
    private BigDecimal taxTotal;
    private BigDecimal netTotal;
    private BigDecimal amountPaid;
    private String status;
}
