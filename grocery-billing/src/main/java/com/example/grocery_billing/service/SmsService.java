package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.entity.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class SmsService {

    /**
     * Mocks sending an SMS/WhatsApp notification to a customer when a bill is generated.
     */
    public void sendBillSms(Customer customer, Bill bill, String shopName) {
        if (customer == null || customer.getPhone() == null || customer.getPhone().trim().isEmpty()) {
            log.info("Skipping SMS: Customer is walk-in or has no phone number.");
            return;
        }

        BigDecimal total = bill.getTotalAmount() != null ? bill.getTotalAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        String message = String.format("Dear %s, thank you for shopping at %s! Your bill (%s) for Rs %s has been generated.",
                customer.getName(), shopName, bill.getBillNo(), total.toPlainString());

        // In a real implementation, you would call the Twilio API or WhatsApp Business API here.
        log.info("=====================================================");
        log.info("MOCK SMS / WHATSAPP SENT");
        log.info("To: {}", customer.getPhone());
        log.info("Message: {}", message);
        log.info("=====================================================");
    }
}
