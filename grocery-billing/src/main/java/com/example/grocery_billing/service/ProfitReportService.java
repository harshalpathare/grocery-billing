package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.entity.BillItem;
import com.example.grocery_billing.repository.BillRepository;
import com.example.grocery_billing.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfitReportService {

    private final BillRepository         billRepository;
    private final PurchaseOrderRepository poRepository;

    // ─────────────────────────────────────────────────────
    // MAIN REPORT DTO
    // ─────────────────────────────────────────────────────
    public static class ProfitReport {
        // Summary
        public BigDecimal totalRevenue     = BigDecimal.ZERO;
        public BigDecimal totalCost        = BigDecimal.ZERO;
        public BigDecimal totalProfit      = BigDecimal.ZERO;
        public BigDecimal profitMargin     = BigDecimal.ZERO;
        public BigDecimal totalPurchases   = BigDecimal.ZERO;

        // Per product breakdown
        public List<ProductProfitRow> productRows =
                new ArrayList<>();

        // Per day breakdown
        public List<DailyProfitRow> dailyRows =
                new ArrayList<>();

        // Counts
        public int totalBills    = 0;
        public int totalProducts = 0;
    }

    public static class ProductProfitRow {
        public String     productName;
        public BigDecimal qtySold      = BigDecimal.ZERO;
        public BigDecimal sellPrice    = BigDecimal.ZERO;
        public BigDecimal costPrice    = BigDecimal.ZERO;
        public BigDecimal totalRevenue = BigDecimal.ZERO;
        public BigDecimal totalCost    = BigDecimal.ZERO;
        public BigDecimal totalProfit  = BigDecimal.ZERO;
        public BigDecimal margin       = BigDecimal.ZERO;
        public String     profitClass; // css class
    }

    public static class DailyProfitRow {
        public LocalDate  date;
        public BigDecimal revenue = BigDecimal.ZERO;
        public BigDecimal cost    = BigDecimal.ZERO;
        public BigDecimal profit  = BigDecimal.ZERO;
        public int        bills   = 0;
    }

    // ─────────────────────────────────────────────────────
    // GENERATE REPORT
    // ─────────────────────────────────────────────────────
    public ProfitReport generateReport(LocalDate start,
                                       LocalDate end) {
        ProfitReport report = new ProfitReport();

        // Get all bills in date range
        List<Bill> bills = billRepository
                .findByBillDateBetweenOrderByBillDateDesc(
                        start, end);

        report.totalBills = bills.size();

        // Map: productId → ProductProfitRow
        Map<Long, ProductProfitRow> productMap =
                new LinkedHashMap<>();

        // Map: date → DailyProfitRow
        Map<LocalDate, DailyProfitRow> dailyMap =
                new TreeMap<>();

        for (Bill bill : bills) {
            // Skip credit bills — no revenue received yet
            // Count partial bills partially
            BigDecimal billMultiplier = BigDecimal.ONE;
            if (Bill.PaymentStatus.CREDIT.equals(
                    bill.getPaymentStatus())) {
                continue; // skip pure credit
            }

            for (BillItem item : bill.getBillItems()) {
                if (item.getProduct() == null) continue;

                Long      pid      = item.getProduct().getId();
                String    pname    = item.getProductNameSnapshot() != null
                        ? item.getProductNameSnapshot()
                        : item.getProduct().getNameEn();
                BigDecimal qty     = item.getQuantity() != null
                        ? item.getQuantity() : BigDecimal.ZERO;
                BigDecimal sellPr  = item.getUnitPrice() != null
                        ? item.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal costPr  = item.getProduct()
                        .getCostPrice() != null
                        ? item.getProduct().getCostPrice()
                        : BigDecimal.ZERO;

                BigDecimal revenue = qty.multiply(sellPr)
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal cost    = qty.multiply(costPr)
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal profit  = revenue.subtract(cost);

                // ── Product row ───────────────────────
                ProductProfitRow row = productMap
                        .computeIfAbsent(pid, k -> {
                            ProductProfitRow r = new ProductProfitRow();
                            r.productName = pname;
                            r.sellPrice   = sellPr;
                            r.costPrice   = costPr;
                            return r;
                        });
                row.qtySold      = row.qtySold.add(qty);
                row.totalRevenue = row.totalRevenue.add(revenue);
                row.totalCost    = row.totalCost.add(cost);
                row.totalProfit  = row.totalProfit.add(profit);

                // ── Report totals ─────────────────────
                report.totalRevenue =
                        report.totalRevenue.add(revenue);
                report.totalCost    =
                        report.totalCost.add(cost);
                report.totalProfit  =
                        report.totalProfit.add(profit);
            }

            // ── Daily row ─────────────────────────────
            LocalDate   date     = bill.getBillDate();
            BigDecimal  billRev  = bill.getTotalAmount() != null
                    ? bill.getTotalAmount() : BigDecimal.ZERO;

            DailyProfitRow daily = dailyMap
                    .computeIfAbsent(date, k -> {
                        DailyProfitRow d = new DailyProfitRow();
                        d.date = k;
                        return d;
                    });
            daily.bills++;
            daily.revenue = daily.revenue.add(billRev);
        }

        // ── Calculate margins per product ─────────────
        for (ProductProfitRow row : productMap.values()) {
            if (row.totalRevenue.compareTo(
                    BigDecimal.ZERO) > 0) {
                row.margin = row.totalProfit
                        .multiply(BigDecimal.valueOf(100))
                        .divide(row.totalRevenue, 1,
                                RoundingMode.HALF_UP);
            }
            // CSS class for color coding
            if (row.totalProfit.compareTo(
                    BigDecimal.ZERO) > 0) {
                row.profitClass = "text-success";
            } else if (row.totalProfit.compareTo(
                    BigDecimal.ZERO) < 0) {
                row.profitClass = "text-danger";
            } else {
                row.profitClass = "text-muted";
            }
        }

        // ── Calculate daily costs ─────────────────────
        // Distribute total cost proportionally to daily revenue
        if (report.totalRevenue.compareTo(
                BigDecimal.ZERO) > 0) {
            for (DailyProfitRow daily : dailyMap.values()) {
                BigDecimal ratio = daily.revenue
                        .divide(report.totalRevenue, 6,
                                RoundingMode.HALF_UP);
                daily.cost   = report.totalCost
                        .multiply(ratio)
                        .setScale(2, RoundingMode.HALF_UP);
                daily.profit = daily.revenue
                        .subtract(daily.cost);
            }
        }

        // ── Overall margin ────────────────────────────
        if (report.totalRevenue.compareTo(
                BigDecimal.ZERO) > 0) {
            report.profitMargin = report.totalProfit
                    .multiply(BigDecimal.valueOf(100))
                    .divide(report.totalRevenue, 1,
                            RoundingMode.HALF_UP);
        }

        // ── Total purchases in period ─────────────────
        BigDecimal purchases = poRepository
                .getTotalPurchasesBetween(start, end);
        report.totalPurchases = purchases != null
                ? purchases : BigDecimal.ZERO;

        // ── Sort products by profit desc ──────────────
        report.productRows = productMap.values().stream()
                .sorted(Comparator.comparing(
                                (ProductProfitRow r) -> r.totalProfit)
                        .reversed())
                .collect(Collectors.toList());

        report.dailyRows = new ArrayList<>(
                dailyMap.values());
        Collections.reverse(report.dailyRows);
        report.totalProducts = report.productRows.size();

        log.info("P&L Report {}-{}: Revenue={}, Cost={}, " +
                        "Profit={}", start, end,
                report.totalRevenue,
                report.totalCost,
                report.totalProfit);

        return report;
    }
    // ─────────────────────────────────────────────────────
// EXCEL EXPORT
// ─────────────────────────────────────────────────────
    public byte[] generateProfitExcel(ProfitReport report,
                                      String monthName)
            throws Exception {

        XSSFWorkbook wb = new XSSFWorkbook();

        // ── Styles ────────────────────────────────────────
        CellStyle titleStyle = wb.createCellStyle();
        XSSFFont titleFont   = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleStyle.setFont(titleFont);
        titleStyle.setFillForegroundColor(
                new XSSFColor(
                        new byte[]{(byte)26,(byte)31,(byte)46},
                        null));
        titleStyle.setFillPattern(
                FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle headerStyle = wb.createCellStyle();
        XSSFFont headerFont   = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(
                new XSSFColor(
                        new byte[]{(byte)26,(byte)31,(byte)46},
                        null));
        headerStyle.setFillPattern(
                FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);

        CellStyle dataStyle = wb.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBottomBorderColor(
                IndexedColors.GREY_25_PERCENT.getIndex());

        CellStyle amtStyle = wb.createCellStyle();
        amtStyle.setDataFormat(
                wb.createDataFormat().getFormat("#,##0.00"));
        amtStyle.setBorderBottom(BorderStyle.THIN);
        amtStyle.setBottomBorderColor(
                IndexedColors.GREY_25_PERCENT.getIndex());
        amtStyle.setAlignment(HorizontalAlignment.RIGHT);

        CellStyle greenAmt = wb.createCellStyle();
        greenAmt.cloneStyleFrom(amtStyle);
        XSSFFont greenFont = wb.createFont();
        greenFont.setBold(true);
        greenFont.setColor(new XSSFColor(
                new byte[]{(byte)21,(byte)128,(byte)61}, null));
        greenAmt.setFont(greenFont);

        CellStyle redAmt = wb.createCellStyle();
        redAmt.cloneStyleFrom(amtStyle);
        XSSFFont redFont = wb.createFont();
        redFont.setBold(true);
        redFont.setColor(new XSSFColor(
                new byte[]{(byte)185,(byte)28,(byte)28}, null));
        redAmt.setFont(redFont);

        CellStyle totalStyle = wb.createCellStyle();
        XSSFFont totalFont   = wb.createFont();
        totalFont.setBold(true);
        totalStyle.setFont(totalFont);
        totalStyle.setFillForegroundColor(
                new XSSFColor(
                        new byte[]{(byte)240,(byte)242,(byte)245},
                        null));
        totalStyle.setFillPattern(
                FillPatternType.SOLID_FOREGROUND);
        totalStyle.setDataFormat(
                wb.createDataFormat().getFormat("#,##0.00"));
        totalStyle.setBorderTop(BorderStyle.MEDIUM);
        totalStyle.setAlignment(HorizontalAlignment.RIGHT);

        // ── Sheet 1: Summary ──────────────────────────────
        XSSFSheet summary = wb.createSheet("P&L Summary");
        summary.setColumnWidth(0, 7000);
        summary.setColumnWidth(1, 5000);

        int r = 0;
        Row titleRow = summary.createRow(r++);
        Cell tc = titleRow.createCell(0);
        tc.setCellValue("Profit & Loss Report — " + monthName);
        tc.setCellStyle(titleStyle);
        summary.addMergedRegion(
                new CellRangeAddress(0,0,0,1));

        r++; // blank

        Object[][] summaryData = {
                { "Total Revenue",       report.totalRevenue },
                { "Total Cost (COGS)",   report.totalCost    },
                { "Net Profit",          report.totalProfit  },
                { "Profit Margin (%)",   report.profitMargin },
                { "Total Bills",
                        BigDecimal.valueOf(report.totalBills)       },
                { "Total Products Sold",
                        BigDecimal.valueOf(report.totalProducts)    },
        };

        for (Object[] sd : summaryData) {
            Row row  = summary.createRow(r++);
            Cell lbl = row.createCell(0);
            lbl.setCellValue((String) sd[0]);
            lbl.setCellStyle(dataStyle);
            Cell val = row.createCell(1);
            val.setCellValue(
                    ((BigDecimal) sd[1]).doubleValue());

            // Color profit green/red
            if (sd[0].equals("Net Profit")) {
                val.setCellStyle(
                        report.totalProfit.compareTo(
                                BigDecimal.ZERO) >= 0
                                ? greenAmt : redAmt);
            } else {
                val.setCellStyle(amtStyle);
            }
        }

        // ── Sheet 2: Product-wise ─────────────────────────
        XSSFSheet products =
                wb.createSheet("Product-wise Profit");
        int[] pWidths = {
                500, 5000, 3000, 3000, 3000,
                3500, 3500, 3500, 2500
        };
        for (int i = 0; i < pWidths.length; i++) {
            products.setColumnWidth(i, pWidths[i]);
        }

        Row ph = products.createRow(0);
        String[] pCols = {
                "#", "Product", "Qty Sold",
                "Sell Price", "Cost Price",
                "Revenue (₹)", "Cost (₹)",
                "Profit (₹)", "Margin %"
        };
        for (int i = 0; i < pCols.length; i++) {
            Cell c = ph.createCell(i);
            c.setCellValue(pCols[i]);
            c.setCellStyle(headerStyle);
        }

        int pr = 1;
        for (ProductProfitRow row : report.productRows) {
            Row pRow = products.createRow(pr);

            Cell srCell = pRow.createCell(0);
            srCell.setCellValue(pr);
            srCell.setCellStyle(dataStyle);

            Cell nameCell = pRow.createCell(1);
            nameCell.setCellValue(row.productName);
            nameCell.setCellStyle(dataStyle);

            Cell qtyCell = pRow.createCell(2);
            qtyCell.setCellValue(row.qtySold.doubleValue());
            qtyCell.setCellStyle(amtStyle);

            Cell spCell = pRow.createCell(3);
            spCell.setCellValue(row.sellPrice.doubleValue());
            spCell.setCellStyle(amtStyle);

            Cell cpCell = pRow.createCell(4);
            cpCell.setCellValue(row.costPrice.doubleValue());
            cpCell.setCellStyle(amtStyle);

            Cell revCell = pRow.createCell(5);
            revCell.setCellValue(
                    row.totalRevenue.doubleValue());
            revCell.setCellStyle(amtStyle);

            Cell costCell = pRow.createCell(6);
            costCell.setCellValue(row.totalCost.doubleValue());
            costCell.setCellStyle(redAmt);

            Cell profCell = pRow.createCell(7);
            profCell.setCellValue(
                    row.totalProfit.doubleValue());
            profCell.setCellStyle(
                    row.totalProfit.compareTo(
                            BigDecimal.ZERO) >= 0
                            ? greenAmt : redAmt);

            Cell marCell = pRow.createCell(8);
            marCell.setCellValue(row.margin.doubleValue());
            marCell.setCellStyle(amtStyle);

            pr++;
        }

        // Totals row
        Row totRow = products.createRow(pr);
        Cell totLbl = totRow.createCell(0);
        totLbl.setCellValue("TOTAL");
        totLbl.setCellStyle(totalStyle);
        for (int i = 1; i <= 4; i++) {
            totRow.createCell(i).setCellStyle(totalStyle);
        }
        Cell totRev = totRow.createCell(5);
        totRev.setCellValue(
                report.totalRevenue.doubleValue());
        totRev.setCellStyle(totalStyle);

        Cell totCost = totRow.createCell(6);
        totCost.setCellValue(report.totalCost.doubleValue());
        totCost.setCellStyle(totalStyle);

        Cell totProfit = totRow.createCell(7);
        totProfit.setCellValue(
                report.totalProfit.doubleValue());
        totProfit.setCellStyle(totalStyle);

        Cell totMargin = totRow.createCell(8);
        totMargin.setCellValue(
                report.profitMargin.doubleValue());
        totMargin.setCellStyle(totalStyle);

        // ── Sheet 3: Daily ────────────────────────────────
        XSSFSheet daily = wb.createSheet("Daily Breakdown");
        daily.setColumnWidth(0, 4000);
        daily.setColumnWidth(1, 2500);
        daily.setColumnWidth(2, 3500);
        daily.setColumnWidth(3, 3500);
        daily.setColumnWidth(4, 3500);

        Row dh = daily.createRow(0);
        String[] dCols = {
                "Date", "Bills",
                "Revenue (₹)", "Est. Cost (₹)",
                "Est. Profit (₹)"
        };
        for (int i = 0; i < dCols.length; i++) {
            Cell c = dh.createCell(i);
            c.setCellValue(dCols[i]);
            c.setCellStyle(headerStyle);
        }

        int dr = 1;
        for (DailyProfitRow dRow : report.dailyRows) {
            Row row = daily.createRow(dr++);

            Cell dateCell = row.createCell(0);
            dateCell.setCellValue(dRow.date.format(
                    java.time.format.DateTimeFormatter
                            .ofPattern("dd MMM yyyy")));
            dateCell.setCellStyle(dataStyle);

            Cell billsCell = row.createCell(1);
            billsCell.setCellValue(dRow.bills);
            billsCell.setCellStyle(dataStyle);

            Cell revCell = row.createCell(2);
            revCell.setCellValue(dRow.revenue.doubleValue());
            revCell.setCellStyle(amtStyle);

            Cell costCell = row.createCell(3);
            costCell.setCellValue(dRow.cost.doubleValue());
            costCell.setCellStyle(redAmt);

            Cell profCell = row.createCell(4);
            profCell.setCellValue(dRow.profit.doubleValue());
            profCell.setCellStyle(
                    dRow.profit.compareTo(
                            BigDecimal.ZERO) >= 0
                            ? greenAmt : redAmt);
        }

        // Write to bytes
        ByteArrayOutputStream baos =
                new ByteArrayOutputStream();
        wb.write(baos);
        wb.close();
        return baos.toByteArray();
    }
}