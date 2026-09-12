package com.example.grocery_billing.dto.api;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ApiBillRequestDto {
    private Long customerId; // Optional, null for walk-in
    private String walkInCustomerName;
    private Boolean isGst;
    private BigDecimal discount;
    private BigDecimal transportCost;
    private BigDecimal extraCost;
    private String paymentStatus; // "PAID", "CREDIT", "PARTIAL"
    private String paymentMethod; // "CASH", "UPI", "CARD"
    private BigDecimal amountPaid;
    private String notes;
    private List<ApiBillItemRequestDto> items;
}
