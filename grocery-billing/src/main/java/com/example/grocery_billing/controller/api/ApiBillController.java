package com.example.grocery_billing.controller.api;

import com.example.grocery_billing.dto.api.ApiBillResponseDto;
import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
public class ApiBillController {

    private final BillService billService;

    @GetMapping
    public ResponseEntity<List<ApiBillResponseDto>> getAllBills() {
        List<Bill> bills = billService.getAllBills();
        
        List<ApiBillResponseDto> response = bills.stream().map(bill -> {
            String customerName = bill.getCustomer() != null ? bill.getCustomer().getName() : bill.getWalkInCustomerName();
            return ApiBillResponseDto.builder()
                    .id(bill.getId())
                    .billNo(bill.getBillNo())
                    .billDate(bill.getBillDate())
                    .customerName(customerName)
                    .subTotal(bill.getSubtotal())
                    .taxTotal(bill.getTotalTax())
                    .netTotal(bill.getTotalAmount())
                    .amountPaid(java.math.BigDecimal.ZERO)
                    .status(bill.getPaymentStatus() != null ? bill.getPaymentStatus().name() : "UNKNOWN")
                    .build();
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @org.springframework.web.bind.annotation.PostMapping
    public ResponseEntity<ApiBillResponseDto> createBill(@org.springframework.web.bind.annotation.RequestBody com.example.grocery_billing.dto.api.ApiBillRequestDto request) {
        Bill bill = new Bill();
        if (request.getCustomerId() != null) {
            com.example.grocery_billing.entity.Customer customer = new com.example.grocery_billing.entity.Customer();
            customer.setId(request.getCustomerId());
            bill.setCustomer(customer);
        } else {
            bill.setWalkInCustomerName(request.getWalkInCustomerName());
        }
        bill.setIsGst(request.getIsGst() != null ? request.getIsGst() : false);
        bill.setDiscount(request.getDiscount());
        bill.setTransportCost(request.getTransportCost());
        bill.setExtraCost(request.getExtraCost());
        
        if (request.getPaymentStatus() != null) {
            bill.setPaymentStatus(Bill.PaymentStatus.valueOf(request.getPaymentStatus()));
        } else {
            bill.setPaymentStatus(Bill.PaymentStatus.PAID);
        }
        
        bill.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CASH");
        bill.setNotes(request.getNotes());

        List<BillService.BillItemRequest> items = request.getItems().stream()
                .map(item -> new BillService.BillItemRequest(
                        item.getProductId(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        "en",
                        "piece",
                        item.getIsReturn()
                )).collect(Collectors.toList());

        java.math.BigDecimal paidAmount = request.getAmountPaid();
        if (paidAmount == null) paidAmount = java.math.BigDecimal.ZERO;
        java.math.BigDecimal creditAmount = java.math.BigDecimal.ZERO;
        if (bill.getPaymentStatus() == Bill.PaymentStatus.CREDIT) {
            // we let the service calculate it, or we assume paid is 0
            paidAmount = java.math.BigDecimal.ZERO;
        }

        Bill saved = billService.createBill(bill, items, paidAmount, creditAmount);

        String customerName = saved.getCustomer() != null ? saved.getCustomer().getName() : saved.getWalkInCustomerName();
        ApiBillResponseDto response = ApiBillResponseDto.builder()
                .id(saved.getId())
                .billNo(saved.getBillNo())
                .billDate(saved.getBillDate())
                .customerName(customerName)
                .subTotal(saved.getSubtotal())
                .taxTotal(saved.getTotalTax())
                .netTotal(saved.getTotalAmount())
                .amountPaid(java.math.BigDecimal.ZERO) // Simplified
                .status(saved.getPaymentStatus() != null ? saved.getPaymentStatus().name() : "UNKNOWN")
                .build();
                
        return ResponseEntity.ok(response);
    }
}
