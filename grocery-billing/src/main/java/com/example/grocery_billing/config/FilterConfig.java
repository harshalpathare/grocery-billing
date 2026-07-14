package com.example.grocery_billing.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<ShopContextFilter> registration(ShopContextFilter filter) {
        FilterRegistrationBean<ShopContextFilter> registration = new FilterRegistrationBean<>(filter);
        // Disable automatic global registration because we add it manually in SecurityConfig
        registration.setEnabled(false);
        return registration;
    }
}
