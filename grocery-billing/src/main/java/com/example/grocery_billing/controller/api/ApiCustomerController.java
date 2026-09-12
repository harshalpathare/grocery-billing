package com.example.grocery_billing.controller.api;

import com.example.grocery_billing.dto.api.ApiCustomerResponseDto;
import com.example.grocery_billing.entity.Customer;
import com.example.grocery_billing.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class ApiCustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<List<ApiCustomerResponseDto>> getAllCustomers() {
        List<Customer> customers = customerService.getAllActiveCustomers();
        List<ApiCustomerResponseDto> response = customers.stream().map(c -> ApiCustomerResponseDto.builder()
                .id(c.getId())
                .name(c.getName())
                .phone(c.getPhone())
                .balance(c.getBalance())
                .build()
        ).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.PostMapping
    public ResponseEntity<ApiCustomerResponseDto> createCustomer(@org.springframework.web.bind.annotation.RequestBody com.example.grocery_billing.dto.api.ApiCustomerRequestDto request) {
        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setEmail(request.getEmail());
        customer.setAddress(request.getAddress());
        customer.setGstin(request.getGstin());
        
        Customer saved = customerService.saveCustomer(customer);
        
        ApiCustomerResponseDto response = ApiCustomerResponseDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .phone(saved.getPhone())
                .balance(saved.getBalance())
                .build();
                
        return ResponseEntity.ok(response);
    }
}
