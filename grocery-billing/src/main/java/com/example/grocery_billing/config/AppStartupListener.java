package com.example.grocery_billing.config;

import com.example.grocery_billing.service
        .SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event
        .ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * On startup — load settings from DB into ShopConfig.
 * This ensures DB settings override application.properties.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppStartupListener {

    private final SystemSettingService settingService;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            settingService.syncToShopConfig();
            log.info("✅ Settings synced from DB" +
                    " to ShopConfig on startup");
        } catch (Exception e) {
            log.warn("Settings sync skipped " +
                            "(DB may not be ready): {}",
                    e.getMessage());
        }
    }
}