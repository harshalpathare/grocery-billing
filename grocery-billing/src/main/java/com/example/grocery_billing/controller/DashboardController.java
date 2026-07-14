package com.example.grocery_billing.controller;

import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.entity.Customer;
import com.example.grocery_billing.entity.Product;
import com.example.grocery_billing.entity.Shop;
import com.example.grocery_billing.service.BillService;
import com.example.grocery_billing.service.CustomerService;
import com.example.grocery_billing.service.ProductService;
import com.example.grocery_billing.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ProductService  productService;
    private final CustomerService customerService;
    private final BillService     billService;
    private final ShopService     shopService;

    @GetMapping("/")
    public String dashboard(Model model, org.springframework.security.core.Authentication auth) {

        // Super Admins don't have a shop, redirect them to their panel
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            return "redirect:/super/shops";
        }

        // ── Shop info from DB ─────────────────────────
        Shop shop = shopService.getCurrentShop().orElse(null);
        String shopName = (shop != null) ? shop.getShopName() : "My Grocery Store";
        model.addAttribute("shop",     shop);
        model.addAttribute("shopName", shopName);

        // ── Basic stats ───────────────────────────────
        model.addAttribute("totalProducts",  productService.countActiveProducts());
        model.addAttribute("totalCustomers", customerService.countActiveCustomers());
        model.addAttribute("totalPending",   customerService.getTotalPendingBalance());
        model.addAttribute("todaySales",     billService.getTodaySales());
        model.addAttribute("todayBills",     billService.getTodayBillCount());

        // ── Customers with dues ───────────────────────
        List<Customer> customersWithDues = customerService.getCustomersWithPendingBalance();
        model.addAttribute("customersWithDues",      customersWithDues);
        model.addAttribute("customersWithDuesCount", customersWithDues.size());

        // ── Recent bills (last 8) ─────────────────────
        List<Bill> recentBills = billService.getAllBills()
                .stream().limit(8).collect(Collectors.toList());
        model.addAttribute("recentBills", recentBills);

        // ── Low stock products (stock <= 5) ───────────
        List<Product> lowStock = productService.getAllActiveProducts()
                .stream()
                .filter(p -> p.getStockQty() != null
                        && p.getStockQty().compareTo(BigDecimal.ZERO) > 0
                        && p.getStockQty().compareTo(BigDecimal.valueOf(5)) <= 0)
                .collect(Collectors.toList());
        model.addAttribute("lowStockProducts", lowStock);

        return "dashboard/index";
    }
}
