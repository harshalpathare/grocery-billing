package com.example.grocery_billing.controller;

import com.example.grocery_billing.service.ProfitPredictorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/profit")
@RequiredArgsConstructor
public class ProfitController {

    private final ProfitPredictorService profitService;

    @GetMapping
    public String profitPage(
            @RequestParam(defaultValue = "30") int days,
            Model model) {

        model.addAttribute("monthlyProfit",
                profitService.getMonthlyProfitAnalysis(6));
        model.addAttribute("prediction",
                profitService.predictNextMonth());
        model.addAttribute("topProducts",
                profitService.getTopProfitProducts(days));
        model.addAttribute("summary",
                profitService.getOverallSummary());
        model.addAttribute("days",       days);
        model.addAttribute("activePage", "profit");
        model.addAttribute("pageTitle",  "Profit Predictor");
        return "profit/index";
    }

    @GetMapping("/api/predict")
    @ResponseBody
    public Object predict() {
        return profitService.predictNextMonth();
    }
}