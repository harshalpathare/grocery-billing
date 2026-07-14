package com.example.grocery_billing.service;

import com.example.grocery_billing.config.ShopConfig;
import com.example.grocery_billing.config.ShopContext;
import com.example.grocery_billing.entity.Shop;
import com.example.grocery_billing.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * SHOP SERVICE
 * Manages shop data — loading current shop, saving settings, listing all shops.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {

    private final ShopRepository shopRepository;
    private final ShopConfig     shopConfig; // fallback for upiId etc.

    // ─────────────────────────────────────────────────────
    // GET CURRENT SHOP (from ShopContext ThreadLocal)
    // ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<Shop> getCurrentShop() {
        Long shopId = ShopContext.getShopId();
        if (shopId == null) return Optional.empty();
        return shopRepository.findById(shopId);
    }

    /**
     * Returns current shop or throws — use in controllers where shop is mandatory.
     */
    @Transactional(readOnly = true)
    public Shop requireCurrentShop() {
        return getCurrentShop()
                .orElseThrow(() -> new RuntimeException(
                        "No shop in context — user not linked to a shop"));
    }

    // ─────────────────────────────────────────────────────
    // SAVE SHOP SETTINGS (called from SettingsController)
    // ─────────────────────────────────────────────────────

    @Transactional
    public Shop saveSettings(Shop updatedShop) {
        Long shopId = ShopContext.getShopId();
        if (shopId == null)
            throw new RuntimeException("No shop in context");

        Shop existing = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found: " + shopId));

        // Update only the editable fields — never overwrite system fields
        if (updatedShop.getShopName()    != null) existing.setShopName(updatedShop.getShopName().trim());
        if (updatedShop.getOwnerName()   != null) existing.setOwnerName(updatedShop.getOwnerName().trim());
        if (updatedShop.getAddress()     != null) existing.setAddress(updatedShop.getAddress().trim());
        if (updatedShop.getCity()        != null) existing.setCity(updatedShop.getCity().trim());
        if (updatedShop.getState()       != null) existing.setState(updatedShop.getState().trim());
        if (updatedShop.getPincode()     != null) existing.setPincode(updatedShop.getPincode().trim());
        if (updatedShop.getPhone()       != null) existing.setPhone(updatedShop.getPhone().trim());
        if (updatedShop.getEmail()       != null) existing.setEmail(updatedShop.getEmail().trim());
        if (updatedShop.getGstin()       != null) existing.setGstin(updatedShop.getGstin().trim());
        if (updatedShop.getFssaiNo()     != null) existing.setFssaiNo(updatedShop.getFssaiNo().trim());
        if (updatedShop.getUpiId()       != null) existing.setUpiId(updatedShop.getUpiId().trim());
        if (updatedShop.getBankDetails() != null) existing.setBankDetails(updatedShop.getBankDetails().trim());
        if (updatedShop.getThankYouMsg() != null) existing.setThankYouMsg(updatedShop.getThankYouMsg().trim());
        if (updatedShop.getTerms()       != null) existing.setTerms(updatedShop.getTerms().trim());
        if (updatedShop.getLogoUrl()     != null) existing.setLogoUrl(updatedShop.getLogoUrl().trim());

        Shop saved = shopRepository.save(existing);
        log.info("Shop settings updated for shop_id={} ({})", shopId, saved.getShopName());
        return saved;
    }

    // ─────────────────────────────────────────────────────
    // SUPER ADMIN — list/manage all shops
    // ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Shop> getAllShops() {
        return shopRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Shop getShopById(Long id) {
        return shopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop not found: " + id));
    }

    @Transactional
    public Shop saveShop(Shop shop) {
        return shopRepository.save(shop);
    }

    @Transactional
    public void toggleShopActive(Long shopId) {
        Shop shop = getShopById(shopId);
        shop.setActive(!shop.getActive());
        shopRepository.save(shop);
    }

    // ─────────────────────────────────────────────────────
    // HELPER — effective UPI id (DB first, fallback to properties)
    // ─────────────────────────────────────────────────────
    public String getEffectiveUpiId() {
        return getCurrentShop()
                .map(Shop::getUpiId)
                .filter(u -> u != null && !u.isBlank())
                .orElse(shopConfig.getUpiId());
    }
}
