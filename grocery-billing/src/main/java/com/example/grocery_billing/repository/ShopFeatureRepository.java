package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.ShopFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopFeatureRepository extends JpaRepository<ShopFeature, Long> {
    List<ShopFeature> findByShopId(Long shopId);
    Optional<ShopFeature> findByShopIdAndFeatureName(Long shopId, String featureName);
}
