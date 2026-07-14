package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {

    Optional<Shop> findByLicenseKey(String licenseKey);

    boolean existsByLicenseKey(String licenseKey);
}
