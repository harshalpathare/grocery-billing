package com.example.grocery_billing.config;



import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SHOP CONFIGURATION
 *
 * All shop details are read from application.properties.
 * Change them there — no need to touch this Java file.
 *
 * @ConfigurationProperties binds properties with prefix "shop"
 * So shop.name in .properties → shopConfig.getName() in Java
 */
@Component
@ConfigurationProperties(prefix = "shop")
@Data
public class ShopConfig {
    private String name        = "My Grocery Store";
    private String ownerName   = "Shop Owner";
    private String address     = "123 Main Street, City";
    private String phone       = "9999999999";
    private String email       = "";
    private String gstin       = "";          // GST Identification Number
    private String fssaiNo     = "";          // Food license number
    private String thankYouMsg = "Thank you for shopping with us!";
}