package com.example.grocery_billing.controller;

import com.example.grocery_billing.service.SalesInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/insights")
@RequiredArgsConstructor
public class SalesInsightController {

    private final SalesInsightService salesInsightService;

    @GetMapping
    public String insightsPage(
            @RequestParam(defaultValue = "30") int days,
            Model model) {

        model.addAttribute("topProducts",
                salesInsightService.getTopSellingProducts(days));
        model.addAttribute("slowProducts",
                salesInsightService.getSlowMovingProducts(days));
        model.addAttribute("bestDays",
                salesInsightService.getBestSalesDays(days));
        model.addAttribute("topCustomers",
                salesInsightService.getTopCustomers(days));
        model.addAttribute("monthlyTrend",
                salesInsightService.getMonthlySalesTrend(6));
        model.addAttribute("recommendations",
                salesInsightService.getAiRecommendations(days));
        model.addAttribute("stats",
                salesInsightService.getSummaryStats(days));
        model.addAttribute("days",       days);
        model.addAttribute("activePage", "insights");
        model.addAttribute("pageTitle",  "AI Sales Insights");
        return "insights/index";
    }
}