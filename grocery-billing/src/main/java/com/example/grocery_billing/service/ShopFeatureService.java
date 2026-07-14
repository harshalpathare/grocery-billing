package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.ShopFeature;
import com.example.grocery_billing.repository.ShopFeatureRepository;
import com.example.grocery_billing.config.ShopContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.grocery_billing.repository.ShopRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopFeatureService {

    private final ShopFeatureRepository shopFeatureRepository;
    private final ShopRepository shopRepository;

    public Map<String, Boolean> getFeaturesForCurrentShop() {
        Long shopId = ShopContext.getShopId();
        if (shopId == null) {
            return getDefaultFeatures();
        }
        return getFeaturesForShop(shopId);
    }

    public Map<String, Boolean> getFeaturesForShop(Long shopId) {
        List<ShopFeature> features = shopFeatureRepository.findByShopId(shopId);
        Map<String, Boolean> featureMap = features.stream()
                .collect(Collectors.toMap(ShopFeature::getFeatureName, ShopFeature::getIsEnabled));
                
        // Merge with defaults for missing keys
        Map<String, Boolean> fullFeatures = new HashMap<>(getDefaultFeatures());
        fullFeatures.putAll(featureMap);
        
        return fullFeatures;
    }

    @Transactional
    public void saveFeaturesForShop(Long shopId, Map<String, Boolean> featureUpdates) {
        List<ShopFeature> existing = shopFeatureRepository.findByShopId(shopId);
        Map<String, ShopFeature> existingMap = existing.stream()
                .collect(Collectors.toMap(ShopFeature::getFeatureName, f -> f));

        for (Map.Entry<String, Boolean> entry : featureUpdates.entrySet()) {
            String name = entry.getKey();
            Boolean enabled = entry.getValue();
            if (existingMap.containsKey(name)) {
                ShopFeature f = existingMap.get(name);
                f.setIsEnabled(enabled);
                shopFeatureRepository.save(f);
            } else {
                ShopFeature nf = new ShopFeature();
                nf.setShop(shopRepository.getReferenceById(shopId));
                nf.setFeatureName(name);
                nf.setIsEnabled(enabled);
                shopFeatureRepository.save(nf);
            }
        }
    }
    
    private Map<String, Boolean> getDefaultFeatures() {
        Map<String, Boolean> defaults = new HashMap<>();
        defaults.put("enable_credit", true);
        defaults.put("enable_gst", true);
        defaults.put("enable_inventory", true);
        defaults.put("enable_export", true);
        defaults.put("enable_sms", false);
        defaults.put("enable_cash_flow", false);
        defaults.put("enable_loyalty", true);
        defaults.put("enable_accounting", true);
        defaults.put("enable_expenses", true);
        return defaults;
    }
}
