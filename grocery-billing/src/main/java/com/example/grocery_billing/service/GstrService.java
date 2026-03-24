package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.entity.BillItem;
import com.example.grocery_billing.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GSTR-1 EXPORT SERVICE
 *
 * GSTR-1 is a monthly GST return.
 * It has these main sections:
 *
 * Sheet 1 — B2C (Business to Consumer) — walk-in + individual customers
 * Sheet 2 — B2B (Business to Business) — business customers with GSTIN
 * Sheet 3 — HSN Summary — product-wise GST breakdown
 * Sheet 4 — Tax Summary — total CGST + SGST collected
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GstrService {

    private final BillRepository billRepository;

    // ─────────────────────────────────────────────────────
    // MAIN EXPORT METHOD
    // ─────────────────────────────────────────────────────
    public byte[] generateGstr1Excel(int month, int year,
                                     String gstin,
                                     String shopName)
            throws Exception {

        // Get all GST bills for the period
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end   = start.withDayOfMonth(
                start.lengthOfMonth());

        List<Bill> allBills = billRepository
                .findByBillDateBetweenOrderByBillDateDesc(
                        start, end);

        // Only GST bills
        List<Bill> gstBills = allBills.stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsGst()))
                .collect(Collectors.toList());

        log.info("GSTR-1 export: {}/{} — {} GST bills found",
                month, year, gstBills.size());

        // Create workbook
        XSSFWorkbook wb = new XSSFWorkbook();

        // Create all sheets
        createCoverSheet(wb, month, year, gstin,
                shopName, gstBills);
        createB2CSheet(wb, gstBills);
        createHsnSheet(wb, gstBills);
        createTaxSummarySheet(wb, gstBills);

        // Write to bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wb.write(baos);
        wb.close();
        return baos.toByteArray();
    }

    // ─────────────────────────────────────────────────────
    // SHEET 1: COVER / SUMMARY
    // ─────────────────────────────────────────────────────
    private void createCoverSheet(XSSFWorkbook wb,
                                  int month, int year,
                                  String gstin,
                                  String shopName,
                                  List<Bill> bills) {

        XSSFSheet sheet = wb.createSheet("GSTR-1 Summary");
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 6000);

        CellStyle titleStyle = createTitleStyle(wb);
        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle dataStyle   = createDataStyle(wb);
        CellStyle amtStyle    = createAmountStyle(wb);

        int r = 0;

        // Title
        Row title = sheet.createRow(r++);
        Cell tc = title.createCell(0);
        tc.setCellValue("GSTR-1 — Monthly Return");
        tc.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0,0,0,1));

        r++; // blank row

        // Period info
        String[] labels = {
                "GSTIN", gstin != null ? gstin : "NOT SET",
                "Trade Name", shopName,
                "Return Period",
                LocalDate.of(year, month, 1)
                        .format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                "Filing Status", "PENDING",
                "", "",
                "Total GST Bills", String.valueOf(bills.size()),
        };

        for (int i = 0; i < labels.length; i += 2) {
            Row row = sheet.createRow(r++);
            Cell lbl = row.createCell(0);
            lbl.setCellValue(labels[i]);
            lbl.setCellStyle(headerStyle);
            Cell val = row.createCell(1);
            val.setCellValue(labels[i+1]);
            val.setCellStyle(dataStyle);
        }

        r++; // blank

        // Totals summary
        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst    = BigDecimal.ZERO;
        BigDecimal totalSgst    = BigDecimal.ZERO;
        BigDecimal totalAmt     = BigDecimal.ZERO;

        for (Bill bill : bills) {
            if (bill.getGstAmount() != null) {
                BigDecimal half = bill.getGstAmount()
                        .divide(BigDecimal.valueOf(2), 2,
                                RoundingMode.HALF_UP);
                totalCgst = totalCgst.add(half);
                totalSgst = totalSgst.add(half);
                totalTaxable = totalTaxable.add(
                        bill.getSubtotal() != null
                                ? bill.getSubtotal() : BigDecimal.ZERO);
            }
            totalAmt = totalAmt.add(
                    bill.getTotalAmount() != null
                            ? bill.getTotalAmount() : BigDecimal.ZERO);
        }

        // Summary table header
        Row sumHdr = sheet.createRow(r++);
        String[] sumCols = { "Description", "Amount (₹)" };
        for (int i = 0; i < sumCols.length; i++) {
            Cell c = sumHdr.createCell(i);
            c.setCellValue(sumCols[i]);
            c.setCellStyle(headerStyle);
        }

        Object[][] sumRows = {
                { "Total Taxable Turnover", totalTaxable },
                { "Total CGST Collected",   totalCgst },
                { "Total SGST Collected",   totalSgst },
                { "Total Tax Collected",
                        totalCgst.add(totalSgst) },
                { "Grand Total (incl. GST)", totalAmt }
        };

        for (Object[] sr : sumRows) {
            Row row = sheet.createRow(r++);
            Cell lc = row.createCell(0);
            lc.setCellValue((String) sr[0]);
            lc.setCellStyle(dataStyle);
            Cell vc = row.createCell(1);
            vc.setCellValue(
                    ((BigDecimal) sr[1]).doubleValue());
            vc.setCellStyle(amtStyle);
        }
    }

    // ─────────────────────────────────────────────────────
    // SHEET 2: B2C — All retail bills
    // ─────────────────────────────────────────────────────
    private void createB2CSheet(XSSFWorkbook wb,
                                List<Bill> bills) {

        XSSFSheet sheet = wb.createSheet("B2C (Retail Sales)");

        // Column widths
        int[] widths = {
                3000, 5000, 4000, 4000, 3000,
                4000, 4000, 4000, 5000
        };
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i]);
        }

        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle dataStyle   = createDataStyle(wb);
        CellStyle amtStyle    = createAmountStyle(wb);
        CellStyle dateStyle   = createDataStyle(wb);

        // Header row
        Row hdr = sheet.createRow(0);
        String[] cols = {
                "Sr.", "Bill No.", "Bill Date",
                "Customer", "GST Rate %",
                "Taxable Value (₹)", "CGST (₹)",
                "SGST (₹)", "Invoice Value (₹)"
        };
        for (int i = 0; i < cols.length; i++) {
            Cell c = hdr.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(headerStyle);
        }

        // Data rows
        int r = 1;
        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst    = BigDecimal.ZERO;
        BigDecimal totalSgst    = BigDecimal.ZERO;
        BigDecimal totalInvoice = BigDecimal.ZERO;

        for (Bill bill : bills) {
            // Calculate GST for this bill
            BigDecimal subtotal = bill.getSubtotal() != null
                    ? bill.getSubtotal() : BigDecimal.ZERO;
            BigDecimal gstAmt   = bill.getGstAmount() != null
                    ? bill.getGstAmount() : BigDecimal.ZERO;
            BigDecimal cgst     = gstAmt.divide(
                    BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            BigDecimal sgst     = cgst;

            // Calculate effective GST rate
            double gstRate = subtotal.compareTo(BigDecimal.ZERO) > 0
                    ? gstAmt.multiply(BigDecimal.valueOf(100))
                    .divide(subtotal, 2, RoundingMode.HALF_UP)
                    .doubleValue()
                    : 0.0;

            Row row = sheet.createRow(r);

            setCell(row, 0, r, dataStyle);
            setCell(row, 1, bill.getBillNo(), dataStyle);
            setCell(row, 2,
                    bill.getBillDate().format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    dateStyle);
            setCell(row, 3,
                    bill.getCustomer() != null
                            ? bill.getCustomer().getName()
                            : "Walk-in",
                    dataStyle);
            setCell(row, 4, gstRate, amtStyle);
            setCell(row, 5, subtotal.doubleValue(), amtStyle);
            setCell(row, 6, cgst.doubleValue(), amtStyle);
            setCell(row, 7, sgst.doubleValue(), amtStyle);
            setCell(row, 8,
                    bill.getTotalAmount() != null
                            ? bill.getTotalAmount().doubleValue() : 0.0,
                    amtStyle);

            totalTaxable = totalTaxable.add(subtotal);
            totalCgst    = totalCgst.add(cgst);
            totalSgst    = totalSgst.add(sgst);
            totalInvoice = totalInvoice.add(
                    bill.getTotalAmount() != null
                            ? bill.getTotalAmount() : BigDecimal.ZERO);
            r++;
        }

        // Totals row
        Row totRow = sheet.createRow(r);
        CellStyle totalStyle = createTotalStyle(wb);
        setCell(totRow, 0, "TOTAL", totalStyle);
        setCell(totRow, 1, "", totalStyle);
        setCell(totRow, 2, "", totalStyle);
        setCell(totRow, 3, "", totalStyle);
        setCell(totRow, 4, "", totalStyle);
        setCell(totRow, 5, totalTaxable.doubleValue(), totalStyle);
        setCell(totRow, 6, totalCgst.doubleValue(),    totalStyle);
        setCell(totRow, 7, totalSgst.doubleValue(),    totalStyle);
        setCell(totRow, 8, totalInvoice.doubleValue(), totalStyle);
    }

    // ─────────────────────────────────────────────────────
    // SHEET 3: HSN SUMMARY
    // Groups all items by HSN code and GST rate
    // ─────────────────────────────────────────────────────
    private void createHsnSheet(XSSFWorkbook wb,
                                List<Bill> bills) {

        XSSFSheet sheet = wb.createSheet("HSN Summary");
        int[] widths = { 4000, 6000, 3000, 4000, 4000, 4000, 4000 };
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i]);
        }

        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle dataStyle   = createDataStyle(wb);
        CellStyle amtStyle    = createAmountStyle(wb);
        CellStyle totalStyle  = createTotalStyle(wb);

        Row hdr = sheet.createRow(0);
        String[] cols = {
                "HSN Code", "Description",
                "GST Rate %", "Taxable Value (₹)",
                "CGST (₹)", "SGST (₹)", "Total Tax (₹)"
        };
        for (int i = 0; i < cols.length; i++) {
            Cell c = hdr.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(headerStyle);
        }

        // Group items by HSN code
        Map<String, HsnRow> hsnMap = new LinkedHashMap<>();

        for (Bill bill : bills) {
            for (BillItem item : bill.getBillItems()) {
                String hsn = item.getProduct() != null
                        && item.getProduct().getHsnCode() != null
                        ? item.getProduct().getHsnCode()
                        : "0000";
                String desc = item.getProductNameSnapshot() != null
                        ? item.getProductNameSnapshot()
                        : (item.getProduct() != null
                        ? item.getProduct().getNameEn() : "");
                BigDecimal gstPct = item.getGstPercent() != null
                        ? item.getGstPercent() : BigDecimal.ZERO;
                BigDecimal taxable = item.getItemTotal() != null
                        ? item.getItemTotal() : BigDecimal.ZERO;
                BigDecimal gstAmt = taxable
                        .multiply(gstPct)
                        .divide(BigDecimal.valueOf(100), 2,
                                RoundingMode.HALF_UP);

                String key = hsn + "_" + gstPct;
                hsnMap.compute(key, (k, v) -> {
                    if (v == null) v = new HsnRow(hsn, desc,
                            gstPct.doubleValue());
                    v.taxable = v.taxable.add(taxable);
                    v.gst     = v.gst.add(gstAmt);
                    return v;
                });
            }
        }

        int r = 1;
        BigDecimal totTaxable = BigDecimal.ZERO;
        BigDecimal totCgst    = BigDecimal.ZERO;
        BigDecimal totSgst    = BigDecimal.ZERO;

        for (HsnRow hr : hsnMap.values()) {
            BigDecimal cgst = hr.gst.divide(
                    BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            BigDecimal sgst = cgst;

            Row row = sheet.createRow(r++);
            setCell(row, 0, hr.hsn,         dataStyle);
            setCell(row, 1, hr.description, dataStyle);
            setCell(row, 2, hr.gstRate,     amtStyle);
            setCell(row, 3, hr.taxable.doubleValue(), amtStyle);
            setCell(row, 4, cgst.doubleValue(),       amtStyle);
            setCell(row, 5, sgst.doubleValue(),       amtStyle);
            setCell(row, 6, hr.gst.doubleValue(),     amtStyle);

            totTaxable = totTaxable.add(hr.taxable);
            totCgst    = totCgst.add(cgst);
            totSgst    = totSgst.add(sgst);
        }

        Row totRow = sheet.createRow(r);
        setCell(totRow, 0, "TOTAL",  totalStyle);
        setCell(totRow, 1, "",       totalStyle);
        setCell(totRow, 2, "",       totalStyle);
        setCell(totRow, 3, totTaxable.doubleValue(),           totalStyle);
        setCell(totRow, 4, totCgst.doubleValue(),              totalStyle);
        setCell(totRow, 5, totSgst.doubleValue(),              totalStyle);
        setCell(totRow, 6, totCgst.add(totSgst).doubleValue(), totalStyle);
    }

    // ─────────────────────────────────────────────────────
    // SHEET 4: TAX SUMMARY
    // ─────────────────────────────────────────────────────
    private void createTaxSummarySheet(XSSFWorkbook wb,
                                       List<Bill> bills) {

        XSSFSheet sheet = wb.createSheet("Tax Summary");
        sheet.setColumnWidth(0, 5000);
        sheet.setColumnWidth(1, 5000);
        sheet.setColumnWidth(2, 5000);
        sheet.setColumnWidth(3, 5000);

        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle dataStyle   = createDataStyle(wb);
        CellStyle amtStyle    = createAmountStyle(wb);
        CellStyle totalStyle  = createTotalStyle(wb);

        Row hdr = sheet.createRow(0);
        String[] cols = {
                "GST Rate %", "Taxable Amt (₹)",
                "CGST (₹)", "SGST (₹)"
        };
        for (int i = 0; i < cols.length; i++) {
            Cell c = hdr.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(headerStyle);
        }

        // Group by GST rate
        Map<BigDecimal, BigDecimal[]> rateMap =
                new TreeMap<>();

        for (Bill bill : bills) {
            for (BillItem item : bill.getBillItems()) {
                BigDecimal rate = item.getGstPercent() != null
                        ? item.getGstPercent().setScale(2)
                        : BigDecimal.ZERO.setScale(2);
                BigDecimal taxable = item.getItemTotal() != null
                        ? item.getItemTotal() : BigDecimal.ZERO;
                BigDecimal gst = taxable
                        .multiply(rate)
                        .divide(BigDecimal.valueOf(100), 2,
                                RoundingMode.HALF_UP);

                rateMap.compute(rate, (k, v) -> {
                    if (v == null)
                        v = new BigDecimal[]{
                                BigDecimal.ZERO, BigDecimal.ZERO};
                    v[0] = v[0].add(taxable);
                    v[1] = v[1].add(gst);
                    return v;
                });
            }
        }

        int r = 1;
        BigDecimal totTaxable = BigDecimal.ZERO;
        BigDecimal totCgst    = BigDecimal.ZERO;
        BigDecimal totSgst    = BigDecimal.ZERO;

        for (Map.Entry<BigDecimal, BigDecimal[]> e
                : rateMap.entrySet()) {
            BigDecimal cgst = e.getValue()[1].divide(
                    BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            BigDecimal sgst = cgst;

            Row row = sheet.createRow(r++);
            setCell(row, 0,
                    e.getKey().doubleValue(), amtStyle);
            setCell(row, 1,
                    e.getValue()[0].doubleValue(), amtStyle);
            setCell(row, 2, cgst.doubleValue(), amtStyle);
            setCell(row, 3, sgst.doubleValue(), amtStyle);

            totTaxable = totTaxable.add(e.getValue()[0]);
            totCgst    = totCgst.add(cgst);
            totSgst    = totSgst.add(sgst);
        }

        Row totRow = sheet.createRow(r);
        setCell(totRow, 0, "TOTAL",
                totalStyle);
        setCell(totRow, 1, totTaxable.doubleValue(), totalStyle);
        setCell(totRow, 2, totCgst.doubleValue(),    totalStyle);
        setCell(totRow, 3, totSgst.doubleValue(),    totalStyle);
    }

    // ─────────────────────────────────────────────────────
    // HELPER: HSN Row DTO
    // ─────────────────────────────────────────────────────
    private static class HsnRow {
        String     hsn, description;
        double     gstRate;
        BigDecimal taxable = BigDecimal.ZERO;
        BigDecimal gst     = BigDecimal.ZERO;

        HsnRow(String hsn, String description, double gstRate) {
            this.hsn         = hsn;
            this.description = description;
            this.gstRate     = gstRate;
        }
    }

    // ─────────────────────────────────────────────────────
    // HELPER: Cell setters
    // ─────────────────────────────────────────────────────
    private void setCell(Row r, int col, Object val,
                         CellStyle style) {
        Cell c = r.createCell(col);
        if (val instanceof String)
            c.setCellValue((String) val);
        else if (val instanceof Double)
            c.setCellValue((Double) val);
        else if (val instanceof Integer)
            c.setCellValue((Integer) val);
        c.setCellStyle(style);
    }

    // ─────────────────────────────────────────────────────
    // HELPERS: Cell styles
    // ─────────────────────────────────────────────────────
    private CellStyle createTitleStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f  = wb.createFont();
        f.setBold(true); f.setFontHeightInPoints((short) 14);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(
                new XSSFColor(new byte[]{(byte)26,(byte)31,(byte)46},
                        null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f  = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(
                new XSSFColor(new byte[]{(byte)26,(byte)31,(byte)46},
                        null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setBorderBottom(BorderStyle.THIN);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private CellStyle createDataStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        s.setBottomBorderColor(
                IndexedColors.GREY_25_PERCENT.getIndex());
        return s;
    }

    private CellStyle createAmountStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat()
                .getFormat("#,##0.00"));
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        s.setBottomBorderColor(
                IndexedColors.GREY_25_PERCENT.getIndex());
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private CellStyle createTotalStyle(XSSFWorkbook wb) {
        CellStyle s = wb.createCellStyle();
        XSSFFont f  = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setFillForegroundColor(
                new XSSFColor(new byte[]{(byte)240,(byte)242,(byte)245},
                        null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setDataFormat(wb.createDataFormat()
                .getFormat("#,##0.00"));
        s.setBorderTop(BorderStyle.MEDIUM);
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }
}