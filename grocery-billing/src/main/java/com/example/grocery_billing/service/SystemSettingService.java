package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.SystemSetting;
import com.example.grocery_billing.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingService {

    private final SystemSettingRepository repo;
    private final com.example.grocery_billing
            .config.ShopConfig shopConfig;

    // ── Setting keys ──────────────────────────────────
    public static final String SHOP_NAME    = "shop.name";
    public static final String SHOP_ADDRESS = "shop.address";
    public static final String SHOP_PHONE   = "shop.phone";
    public static final String SHOP_EMAIL   = "shop.email";
    public static final String SHOP_GSTIN   = "shop.gstin";
    public static final String SHOP_FSSAI   = "shop.fssai-no";
    public static final String SHOP_UPI_ID  = "shop.upi-id";
    public static final String SHOP_BANK    = "shop.bank-details";
    public static final String INVOICE_FOOTER
            = "shop.invoice-footer";
    public static final String SHOP_LOGO_URL
            = "shop.logo-url";

    // ── GET ───────────────────────────────────────────
    public String get(String key) {
        return repo.findByKey(key)
                .map(SystemSetting::getValue)
                .orElse(null);
    }

    public String get(String key, String defaultValue) {
        String val = get(key);
        return val != null && !val.isBlank()
                ? val : defaultValue;
    }

    // ── SAVE SINGLE ───────────────────────────────────
    @Transactional
    public void set(String key, String value) {
        SystemSetting setting = repo.findByKey(key)
                .orElse(SystemSetting.builder()
                        .key(key).build());
        setting.setValue(value);
        repo.save(setting);
    }

    // ── SAVE ALL FROM FORM ────────────────────────────
    @Transactional
    public void saveAll(Map<String, String> settings) {
        for (Map.Entry<String, String> e
                : settings.entrySet()) {
            set(e.getKey(), e.getValue());
        }

        // ✅ Sync to ShopConfig so changes
        // take effect immediately without restart
        syncToShopConfig();

        log.info("Settings saved and synced: {}",
                settings.keySet());
    }

    // ── SYNC DB → ShopConfig ──────────────────────────
    public void syncToShopConfig() {
        String name = get(SHOP_NAME);
        if (name != null) shopConfig.setName(name);

        String address = get(SHOP_ADDRESS);
        if (address != null)
            shopConfig.setAddress(address);

        String phone = get(SHOP_PHONE);
        if (phone != null) shopConfig.setPhone(phone);

        String gstin = get(SHOP_GSTIN);
        if (gstin != null) shopConfig.setGstin(gstin);

        String fssai = get(SHOP_FSSAI);
        if (fssai != null) shopConfig.setFssaiNo(fssai);

        String upiId = get(SHOP_UPI_ID);
        if (upiId != null) shopConfig.setUpiId(upiId);
    }

    // ── GET ALL AS MAP ────────────────────────────────
    public Map<String, String> getAllAsMap() {
        Map<String, String> map =
                new java.util.LinkedHashMap<>();

        // Fill from DB
        for (SystemSetting s : repo.findAll()) {
            map.put(s.getKey(), s.getValue());
        }

        // Fill missing with ShopConfig defaults
        map.putIfAbsent(SHOP_NAME,
                shopConfig.getName());
        map.putIfAbsent(SHOP_ADDRESS,
                shopConfig.getAddress());
        map.putIfAbsent(SHOP_PHONE,
                shopConfig.getPhone());
        map.putIfAbsent(SHOP_GSTIN,
                shopConfig.getGstin());
        map.putIfAbsent(SHOP_FSSAI,
                shopConfig.getFssaiNo());
        map.putIfAbsent(SHOP_UPI_ID,
                shopConfig.getUpiId());

        return map;
    }
}