package com.example.grocery_billing.dto.api;

import lombok.Data;

@Data
public class ApiCustomerRequestDto {
    private String name;
    private String phone;
    private String email;
    private String address;
    private String gstin;
}
