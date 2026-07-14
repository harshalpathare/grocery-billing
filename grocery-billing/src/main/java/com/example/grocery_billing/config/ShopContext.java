package com.example.grocery_billing.config;

/**
 * SHOP CONTEXT
 * Holds the current shop's ID for the duration of each HTTP request.
 * Uses ThreadLocal so each request thread has its own isolated value.
 *
 * Usage:
 *   ShopContext.getShopId()  → get current shop's ID
 *   ShopContext.setShopId(1L) → set in filter/interceptor
 *   ShopContext.clear()      → clear after request completes
 */
public class ShopContext {

    private static final ThreadLocal<Long> CURRENT_SHOP_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_ROLE  = new ThreadLocal<>();

    public static void setShopId(Long shopId) {
        CURRENT_SHOP_ID.set(shopId);
    }

    public static Long getShopId() {
        return CURRENT_SHOP_ID.get();
    }

    public static void setRole(String role) {
        CURRENT_ROLE.set(role);
    }

    public static String getRole() {
        return CURRENT_ROLE.get();
    }

    public static boolean isSuperAdmin() {
        return "ROLE_SUPER_ADMIN".equals(CURRENT_ROLE.get());
    }

    public static void clear() {
        CURRENT_SHOP_ID.remove();
        CURRENT_ROLE.remove();
    }
}
