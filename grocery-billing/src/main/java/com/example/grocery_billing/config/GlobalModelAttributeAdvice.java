package com.example.grocery_billing.config;

import com.example.grocery_billing.entity.Shop;
import com.example.grocery_billing.service.ShopService;
import com.example.grocery_billing.service.ShopFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributeAdvice {

    private final ShopService shopService;
    private final ShopConfig shopConfig;
    private final ShopFeatureService shopFeatureService;

    @ModelAttribute("requestURI")
    public String populateRequestURI(jakarta.servlet.http.HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("currentShop")
    public Shop populateCurrentShop() {
        return shopService.getCurrentShop().orElseGet(() -> {
            // Fallback for pages without authentication or if shop context is missing
            Shop fallback = new Shop();
            fallback.setShopName(shopConfig.getName());
            fallback.setAddress(shopConfig.getAddress());
            fallback.setPhone(shopConfig.getPhone());
            fallback.setGstin(shopConfig.getGstin());
            fallback.setLogoUrl(shopConfig.getLogoUrl());
            fallback.setThankYouMsg(shopConfig.getThankYouMsg());
            fallback.setTerms(shopConfig.getTerms());
            return fallback;
        });
    }

    @ModelAttribute("features")
    public java.util.Map<String, Boolean> populateFeatures() {
        java.util.Map<String, Boolean> feats = shopFeatureService.getFeaturesForCurrentShop();
        System.out.println("GlobalModelAttributeAdvice: ShopID=" + ShopContext.getShopId() + " Features=" + feats);
        return feats;
    }
}
