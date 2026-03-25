package com.example.grocery_billing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "shop")
@Data  // ✅ @Data adds getters AND setters
public class ShopConfig {

    private String name;
    private String address;
    private String phone;
    private String email;
    private String gstin;
    private String fssaiNo;
    private String upiId;
    private String bankDetails;
    private String thankYouMsg = "Thank you for shopping! Visit again.";
}