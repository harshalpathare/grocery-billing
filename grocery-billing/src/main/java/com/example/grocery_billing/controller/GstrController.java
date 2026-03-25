package com.example.grocery_billing.controller;

import com.example.grocery_billing.config.ShopConfig;
import com.example.grocery_billing.service.GstrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/reports/gstr1")
@RequiredArgsConstructor
@Slf4j
public class GstrController {

    private final GstrService gstrService;
    private final ShopConfig  shopConfig;

    // ─────────────────────────────────────────────────────
    // GSTR-1 PAGE
    // ─────────────────────────────────────────────────────
    @GetMapping
    public String gstr1Page(
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "year", required = false) Integer year,
            Model model) {

        // Default to current month
        LocalDate now = LocalDate.now();
        if (month == null) month = now.getMonthValue();
        if (year  == null) year  = now.getYear();

        model.addAttribute("month",      month);
        model.addAttribute("year",       year);
        model.addAttribute("shopConfig", shopConfig);
        model.addAttribute("activePage", "reports");
        model.addAttribute("pageTitle",  "GSTR-1 Export");

        // Month name for display
        model.addAttribute("monthName",
                LocalDate.of(year, month, 1)
                        .format(DateTimeFormatter
                                .ofPattern("MMMM yyyy")));

        return "report/gstr1-export";
    }

    // ─────────────────────────────────────────────────────
    // DOWNLOAD GSTR-1 EXCEL
    // ─────────────────────────────────────────────────────
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadGstr1(
            @RequestParam("month") int month,
            @RequestParam("year") int year) {

        try {
            byte[] excel = gstrService.generateGstr1Excel(
                    month, year,
                    shopConfig.getGstin(),
                    shopConfig.getName());

            String filename = String.format(
                    "GSTR1_%s_%d_%d.xlsx",
                    shopConfig.getName()
                            .replaceAll("\\s+", "_"),
                    month, year);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument"
                            + ".spreadsheetml.sheet"));
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename(filename).build());

            log.info("GSTR-1 downloaded: {}", filename);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excel);

        } catch (Exception e) {
            log.error("GSTR-1 export failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

