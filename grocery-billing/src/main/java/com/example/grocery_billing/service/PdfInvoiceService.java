package com.example.grocery_billing.service;
// ── Add this import at top ──────────────────────────
import com.example.grocery_billing.service.QrCodeService;
import com.example.grocery_billing.config.ShopConfig;
import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.entity.BillItem;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfInvoiceService {
    // ── Add field ───────────────────────────────────────
    private final QrCodeService qrCodeService;
    private final ShopConfig shopConfig;

    // ── Colors ────────────────────────────────────────────
    private static final Color C_BLACK      = new Color(15,  15,  20);
    private static final Color C_DARK_NAVY  = new Color(30,  30,  30);
    private static final Color C_ACCENT     = new Color(40,  40,  40);
    private static final Color C_LIGHT_BLUE = new Color(240, 240, 240);
    private static final Color C_HEADER_BG  = new Color(255, 255, 255);
    private static final Color C_TABLE_HEAD = new Color(230, 230, 230);
    private static final Color C_ROW_ALT    = new Color(248, 248, 248);
    private static final Color C_BORDER     = new Color(180, 180, 180);
    private static final Color C_GST_BG     = new Color(255, 252, 235);
    private static final Color C_TOTAL_BG   = new Color(30,  30,  30);
    private static final Color C_GREEN      = new Color(20,  130, 60);
    private static final Color C_RED        = new Color(190, 30,  40);
    private static final Color C_MUTED      = new Color(100, 100, 100);
    private static final Color C_WHITE      = Color.WHITE;

    // ── Number-to-words arrays (class level — defined ONCE) ──
    private static final String[] ONES = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven",
            "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen",
            "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty",
            "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Add QR section in generateInvoicePdf() ──────────
// Call this BEFORE addTermsAndSignature(doc)
    private void addUpiQrSection(Document doc, Bill bill) throws Exception {
        if (!qrCodeService.isUpiConfigured()
                || bill.getTotalAmount() == null
                || bill.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String qrBase64 = qrCodeService.generateUpiQrForView(
                bill.getTotalAmount(), bill.getBillNo());

        if (qrBase64 == null) return;

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{30f, 70f});
        t.setSpacingBefore(6);

        // QR image cell
        PdfPCell qrCell = new PdfPCell();
        qrCell.setBorder(Rectangle.BOX);
        qrCell.setBorderColor(C_BORDER);
        qrCell.setBorderWidth(0.8f);
        qrCell.setPadding(8);
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        // Convert base64 back to image
        byte[] qrBytes = java.util.Base64.getDecoder().decode(qrBase64);
        com.lowagie.text.Image qrImg =
                com.lowagie.text.Image.getInstance(qrBytes);
        qrImg.scaleToFit(80, 80);
        qrCell.addElement(qrImg);

        Font qrLabelFont = regular(8, C_MUTED);
        Paragraph qrLabel = new Paragraph("Scan to Pay", qrLabelFont);
        qrLabel.setAlignment(Element.ALIGN_CENTER);
        qrCell.addElement(qrLabel);

        Font upiFont = bold(8, C_ACCENT);
        Paragraph upiLabel = new Paragraph(shopConfig.getUpiId(), upiFont);
        upiLabel.setAlignment(Element.ALIGN_CENTER);
        qrCell.addElement(upiLabel);

        // Instructions cell
        PdfPCell instrCell = new PdfPCell();
        instrCell.setBorder(Rectangle.BOX);
        instrCell.setBorderColor(C_BORDER);
        instrCell.setBorderWidth(0.8f);
        instrCell.setPadding(10);
        instrCell.setBackgroundColor(new Color(245, 245, 245));

        instrCell.addElement(new Paragraph("Pay via UPI", bold(10, C_ACCENT)));
        instrCell.addElement(spacer(4));

        Font stepFont = regular(8, C_BLACK);
        instrCell.addElement(new Paragraph(
                "1. Open GPay / PhonePe / Paytm", stepFont));
        instrCell.addElement(new Paragraph(
                "2. Tap 'Scan QR Code'", stepFont));
        instrCell.addElement(new Paragraph(
                "3. Scan the QR — amount pre-filled", stepFont));
        instrCell.addElement(new Paragraph(
                "4. Confirm and pay", stepFont));
        instrCell.addElement(spacer(4));

        Font upiIdFont = bold(9, C_GREEN);
        instrCell.addElement(new Paragraph(
                "UPI ID: " + shopConfig.getUpiId(), upiIdFont));

        t.addCell(qrCell);
        t.addCell(instrCell);
        doc.add(t);
    }
    // ─────────────────────────────────────────────────────
    //  MAIN ENTRY POINT
    // ─────────────────────────────────────────────────────
    public byte[] generateInvoicePdf(Bill bill) throws Exception {
        loadDevanagariFont();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 28, 28, 28, 28);
        PdfWriter.getInstance(doc, baos);
        doc.addTitle("Invoice " + bill.getBillNo());
        doc.open();

        addShopHeader(doc, bill);
        addCustomerRow(doc, bill);
        addItemsTable(doc, bill);
        addTotalsAndGst(doc, bill);
        addAmountInWords(doc, bill);
        if ("UPI".equals(bill.getPaymentMethod())) {
            //addUpiQrSection(doc, bill);
        }
        addTermsAndSignature(doc);

        doc.close();
        return baos.toByteArray();
    }
    // ── Add these fields after the color constants ─────────
    private static Font FONT_REGULAR_DEVA;  // for Devanagari text
    private static Font FONT_BOLD_DEVA;
    private static boolean devaFontLoaded = false;

// ── Add this method to the class ──────────────────────
    /**
     * Loads a Unicode font that supports Devanagari (Hindi/Marathi).
     * Falls back to Helvetica if font cannot be loaded.
     *
     * We use NotoSansDevanagari from Google Fonts.
     * The font is loaded once and cached as a static field.
     */
    private void loadDevanagariFont() {
        if (devaFontLoaded) return;
        try {
            // Try to load from classpath (if you place font in resources)
            String fontPath = getClass().getClassLoader()
                    .getResource("fonts/NotoSansDevanagari-Regular.ttf") != null
                    ? getClass().getClassLoader()
                    .getResource("fonts/NotoSansDevanagari-Regular.ttf")
                    .getPath()
                    : null;

            if (fontPath != null) {
                BaseFont bf = BaseFont.createFont(
                        fontPath,
                        BaseFont.IDENTITY_H,
                        BaseFont.EMBEDDED);
                FONT_REGULAR_DEVA = new Font(bf, 8,  Font.NORMAL, C_BLACK);
                FONT_BOLD_DEVA    = new Font(bf, 8,  Font.BOLD,   C_BLACK);
            } else {
                // Fallback
                FONT_REGULAR_DEVA = regular(8, C_BLACK);
                FONT_BOLD_DEVA    = bold(8,   C_BLACK);
            }
            devaFontLoaded = true;
        } catch (Exception e) {
            FONT_REGULAR_DEVA = regular(8, C_BLACK);
            FONT_BOLD_DEVA    = bold(8,   C_BLACK);
            devaFontLoaded = true;
        }
    }
    // ─────────────────────────────────────────────────────
    //  SECTION 1 — Shop Header
    // ─────────────────────────────────────────────────────
    private void addShopHeader(Document doc, Bill bill) throws Exception {
        PdfPTable outer = new PdfPTable(1);
        outer.setWidthPercentage(100);
        outer.setSpacingAfter(0);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(C_HEADER_BG);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(C_ACCENT);
        cell.setBorderWidth(1.5f);
        cell.setPaddingTop(10);
        cell.setPaddingBottom(10);

        Paragraph taxLabel = new Paragraph("TAX INVOICE", bold(9, C_MUTED));
        taxLabel.setAlignment(Element.ALIGN_CENTER);
        taxLabel.setSpacingAfter(2);
        cell.addElement(taxLabel);

        if (shopConfig.getLogoUrl() != null && !shopConfig.getLogoUrl().isBlank()) {
            try {
                com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(new java.net.URL(shopConfig.getLogoUrl()));
                logo.scaleToFit(80, 80);
                logo.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(logo);
            } catch (Exception e) {
                // Ignore missing logo
            }
        }

        Paragraph shopName = new Paragraph(shopConfig.getName().toUpperCase(), bold(20, C_BLACK));
        shopName.setAlignment(Element.ALIGN_CENTER);
        shopName.setSpacingAfter(3);
        cell.addElement(shopName);

        Font addrFont = regular(8, new Color(80, 80, 80));

        Paragraph addr = new Paragraph(shopConfig.getAddress(), addrFont);
        addr.setAlignment(Element.ALIGN_CENTER);
        addr.setSpacingAfter(2);
        cell.addElement(addr);

        String contactLine = "Phone: " + shopConfig.getPhone()
                + (shopConfig.getEmail() != null && !shopConfig.getEmail().isEmpty()
                ? "   Email: " + shopConfig.getEmail() : "");
        Paragraph contact = new Paragraph(contactLine, addrFont);
        contact.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(contact);

        outer.addCell(cell);
        doc.add(outer);
    }

    // ─────────────────────────────────────────────────────
    //  SECTION 2 — Customer + Bill Info Row
    // ─────────────────────────────────────────────────────
    private void addCustomerRow(Document doc, Bill bill) throws Exception {
        boolean hasGst = Boolean.TRUE.equals(bill.getIsGst());

        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{45f, 30f, 25f});
        t.setSpacingBefore(0);
        t.setSpacingAfter(0);

        // Left: M/S Customer
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.BOX);
        left.setBorderColor(C_BORDER);
        left.setBorderWidth(0.8f);
        left.setPadding(7);

        left.addElement(new Paragraph(
                "M/S  " + (bill.getCustomer() != null
                        ? bill.getCustomer().getName() : "CASH CUSTOMER"),
                bold(9, C_BLACK)));

        if (bill.getCustomer() != null
                && bill.getCustomer().getAddress() != null
                && !bill.getCustomer().getAddress().isBlank()) {
            left.addElement(new Paragraph(
                    bill.getCustomer().getAddress(), regular(8, C_MUTED)));
        }

        if (hasGst) {
            left.addElement(spacer(3));
            left.addElement(new Paragraph("GST No :", regular(7, C_MUTED)));
            left.addElement(new Paragraph("FSSAI  :", regular(7, C_MUTED)));
        }

        // Middle: Bill No, GST, FSSAI
        PdfPCell mid = new PdfPCell();
        mid.setBorder(Rectangle.BOX);
        mid.setBorderColor(C_BORDER);
        mid.setBorderWidth(0.8f);
        mid.setPadding(7);

        addLabelValue(mid, "Bill No  :", bill.getBillNo(),       9);
        addLabelValue(mid, "GST No  :", shopConfig.getGstin(),   8);
        addLabelValue(mid, "FSSAI   :", shopConfig.getFssaiNo(), 8);

        // Right: Date, Payment, Status
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.BOX);
        right.setBorderColor(C_BORDER);
        right.setBorderWidth(0.8f);
        right.setPadding(7);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);

        addLabelValue(right, "Bill Date :",
                bill.getBillDate().format(DATE_FMT), 9);
        addLabelValue(right, "Payment  :",
                bill.getPaymentMethod() != null
                        ? bill.getPaymentMethod() : "CASH", 8);

        right.addElement(spacer(4));
        Color statusColor = switch (bill.getPaymentStatus()) {
            case PAID   -> C_GREEN;
            case CREDIT -> C_RED;
            default     -> new Color(160, 100, 0);
        };
        Paragraph statusP = new Paragraph(
                "● " + bill.getPaymentStatus().name(), bold(9, statusColor));
        statusP.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(statusP);

        t.addCell(left);
        t.addCell(mid);
        t.addCell(right);
        doc.add(t);
    }

    // ─────────────────────────────────────────────────────
    //  SECTION 3 — Items Table
    // ─────────────────────────────────────────────────────
    private void addItemsTable(Document doc, Bill bill) throws Exception {
        boolean hasGst = Boolean.TRUE.equals(bill.getIsGst());

        PdfPTable t = new PdfPTable(hasGst ? 9 : 6);
        t.setWidthPercentage(100);
        t.setSpacingBefore(0);
        t.setSpacingAfter(0);

        if (hasGst) {
            t.setWidths(new float[]{4f, 30f, 8f, 10f, 9f, 9f, 7f, 8f, 15f});
        } else {
            t.setWidths(new float[]{5f, 42f, 12f, 14f, 12f, 15f});
        }

        Font thF = bold(7, C_ACCENT);

        // ── Header rows ──────────────────────────────────
        if (hasGst) {
            // Row 1: main headers with rowspan=2 except S+C GST which has colspan=2
            PdfPCell srH = thCell("Sr.",         thF, Element.ALIGN_CENTER); srH.setRowspan(2); t.addCell(srH);
            PdfPCell dH  = thCell("Description", thF, Element.ALIGN_LEFT);   dH.setRowspan(2);  t.addCell(dH);
            PdfPCell hH  = thCell("HSN No",      thF, Element.ALIGN_CENTER); hH.setRowspan(2);  t.addCell(hH);
            PdfPCell qH  = thCell("Qty",         thF, Element.ALIGN_CENTER); qH.setRowspan(2);  t.addCell(qH);
            PdfPCell mH  = thCell("MRP",         thF, Element.ALIGN_CENTER); mH.setRowspan(2);  t.addCell(mH);
            PdfPCell rH  = thCell("Rate",        thF, Element.ALIGN_RIGHT);  rH.setRowspan(2);  t.addCell(rH);

            PdfPCell gH  = thCell("S+C GST",     thF, Element.ALIGN_CENTER); gH.setColspan(2);  t.addCell(gH);

            PdfPCell nH  = thCell("Net Amount",  thF, Element.ALIGN_RIGHT);  nH.setRowspan(2);  t.addCell(nH);

            // Row 2: only the 2 GST sub-columns
            t.addCell(thCell("Rate %", bold(7, C_MUTED), Element.ALIGN_CENTER));
            t.addCell(thCell("Amt ₹",  bold(7, C_MUTED), Element.ALIGN_CENTER));

        } else {
            // Non-GST: simple single-row header
            th(t, "Sr.",         thF, Element.ALIGN_CENTER, 1, 0);
            th(t, "Description", thF, Element.ALIGN_LEFT,   1, 0);
            th(t, "Qty",         thF, Element.ALIGN_CENTER, 1, 0);
            th(t, "Rate",        thF, Element.ALIGN_RIGHT,  1, 0);
            th(t, "MRP",         thF, Element.ALIGN_RIGHT,  1, 0);
            th(t, "Net Amount",  thF, Element.ALIGN_RIGHT,  1, 0);
        }

        // ── Data rows ──────────────────────────────────
        Font rowF  = regular(8, C_BLACK);
        Font rowBF = bold(8, C_BLACK);
        Font mutF  = regular(8, C_MUTED);

        int rowNum = 1;
        for (BillItem item : bill.getBillItems()) {
            Color rowBg = (rowNum % 2 == 0) ? C_ROW_ALT : C_WHITE;

            // Sr.
            td(t, String.valueOf(rowNum), mutF, Element.ALIGN_CENTER, rowBg);

            // Description (no word-break)
            // ── Replace the nameCell block with this ──
            String productName = item.getProductNameSnapshot() != null
                    ? item.getProductNameSnapshot()
                    : item.getProduct().getNameEn();

// Detect if name contains Devanagari characters
            boolean isDevanagari = productName.chars()
                    .anyMatch(c -> Character.UnicodeBlock.of(c)
                            == Character.UnicodeBlock.DEVANAGARI);

// Use Unicode font for Devanagari, bold font for Latin
            Font nameFont = isDevanagari ? FONT_BOLD_DEVA : rowBF;

            PdfPCell nameCell = new PdfPCell(new Phrase(productName, nameFont));
            nameCell.setBackgroundColor(rowBg);
            nameCell.setBorderColor(C_BORDER);
            nameCell.setBorderWidth(0.4f);
            nameCell.setPaddingTop(4);
            nameCell.setPaddingBottom(4);
            nameCell.setPaddingLeft(4);
            nameCell.setPaddingRight(4);
            nameCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            nameCell.setNoWrap(false);
            nameCell.setLeading(0f, 1.3f);
            t.addCell(nameCell);
            // HSN (blank — can be extended later)
            if (hasGst) {
                td(t, "", mutF, Element.ALIGN_CENTER, rowBg);
            }

            // Qty
                String unit = item.getProduct() != null && item.getProduct().getUnit() != null
                    ? item.getProduct().getUnit().toUpperCase()
                    : "PIECE";
                String qty = item.getQuantity().stripTrailingZeros().toPlainString()
                    + " " + unit;
            td(t, qty, rowF, Element.ALIGN_CENTER, rowBg);

            // MRP
            td(t, fmt(item.getUnitPrice()), mutF, Element.ALIGN_RIGHT, rowBg);

            // Rate
            td(t, fmt(item.getUnitPrice()), rowF, Element.ALIGN_RIGHT, rowBg);

            if (hasGst) {
                BigDecimal gstPct = item.getGstPercent() != null
                        ? item.getGstPercent() : BigDecimal.ZERO;
                Font gstF = regular(8, new Color(140, 90, 0));

                td(t, gstPct.stripTrailingZeros().toPlainString(),
                        gstF, Element.ALIGN_CENTER, rowBg);

                BigDecimal gstAmt = item.getItemTotal()
                        .multiply(gstPct)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                td(t, fmt(gstAmt), gstF, Element.ALIGN_RIGHT, rowBg);
            }

            // Net Amount
            BigDecimal netAmt = item.getItemTotal();
            if (hasGst && item.getGstPercent() != null) {
                netAmt = netAmt.add(
                        item.getItemTotal()
                                .multiply(item.getGstPercent())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
            td(t, fmt(netAmt), rowBF, Element.ALIGN_RIGHT, rowBg);

            rowNum++;
        }

        // Blank filler rows
        int fillerRows = Math.max(0, 14 - bill.getBillItems().size());
        int cols = hasGst ? 9 : 6;
        for (int i = 0; i < fillerRows; i++) {
            for (int c = 0; c < cols; c++) {
                PdfPCell filler = new PdfPCell(new Phrase(" ", regular(7, C_WHITE)));
                filler.setBorderColor(C_BORDER);
                filler.setBorderWidth(0.4f);
                filler.setMinimumHeight(14f);
                filler.setPadding(3);
                t.addCell(filler);
            }
        }

        doc.add(t);
    }

    // ─────────────────────────────────────────────────────
    //  SECTION 4 — Totals + GST Breakdown
    // ─────────────────────────────────────────────────────
    private void addTotalsAndGst(Document doc, Bill bill) throws Exception {
        boolean hasGst = Boolean.TRUE.equals(bill.getIsGst());

        BigDecimal subtotal   = orZero(bill.getSubtotal());
        BigDecimal totalGst   = orZero(bill.getGstAmount());
        BigDecimal cgst       = totalGst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal sgst       = totalGst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal transport  = orZero(bill.getTransportCost());
        BigDecimal extra      = orZero(bill.getExtraCost());
        BigDecimal discount   = orZero(bill.getDiscount());
        BigDecimal grandTotal = orZero(bill.getTotalAmount());
        BigDecimal rounded    = grandTotal.setScale(0, RoundingMode.HALF_UP).setScale(2);
        BigDecimal roundOff   = rounded.subtract(grandTotal);

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{55f, 45f});
        t.setSpacingBefore(0);

        // ── LEFT: GST Breakdown ──────────────────────
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.BOX);
        left.setBorderColor(C_BORDER);
        left.setBorderWidth(0.8f);
        left.setPadding(6);
        left.setBackgroundColor(C_GST_BG);

        if (hasGst) {
            PdfPTable gstBreak = new PdfPTable(5);
            gstBreak.setWidthPercentage(100);
            gstBreak.setWidths(new float[]{22f, 16f, 20f, 16f, 20f});

            Font ghF = bold(7, C_ACCENT);
            Font gvF = regular(7, C_BLACK);

            miniTh(gstBreak, "Basic Amt", ghF);
            miniTh(gstBreak, "CGST%",     ghF);
            miniTh(gstBreak, "Tax",       ghF);
            miniTh(gstBreak, "SGST%",     ghF);
            miniTh(gstBreak, "Tax",       ghF);

            // Group by GST rate
            Map<BigDecimal, BigDecimal> gstMap = new LinkedHashMap<>();
            for (BillItem item : bill.getBillItems()) {
                BigDecimal rate = orZero(item.getGstPercent());
                gstMap.merge(rate, item.getItemTotal(), BigDecimal::add);
            }

            for (Map.Entry<BigDecimal, BigDecimal> e : gstMap.entrySet()) {
                BigDecimal rate     = e.getKey();
                BigDecimal taxable  = e.getValue();
                BigDecimal halfRate = rate.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                BigDecimal tax      = taxable.multiply(rate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal halfTax  = tax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

                miniTd(gstBreak, fmt(taxable),  gvF);
                miniTd(gstBreak, fmt(halfRate),  gvF);
                miniTd(gstBreak, fmt(halfTax),   gvF);
                miniTd(gstBreak, fmt(halfRate),  gvF);
                miniTd(gstBreak, fmt(halfTax),   gvF);
            }

            left.addElement(gstBreak);
        } else {
            left.addElement(new Paragraph("Non-GST Bill", regular(8, C_MUTED)));
        }

        // ── RIGHT: Totals ──────────────────────────────
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.BOX);
        right.setBorderColor(C_BORDER);
        right.setBorderWidth(0.8f);
        right.setPadding(0);

        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(100);
        totals.setWidths(new float[]{50f, 50f});

        Font tlF = regular(8, C_MUTED);
        Font tvF = bold(8, C_BLACK);

        totalRow(totals, "Total",      fmt(subtotal.add(totalGst)), tlF, tvF, C_WHITE);

        if (hasGst) {
            totalRow(totals, "CGST%", fmt(cgst), tlF, regular(8, new Color(140, 90, 0)), C_GST_BG);
            totalRow(totals, "SGST%", fmt(sgst), tlF, regular(8, new Color(140, 90, 0)), C_GST_BG);
        }

        if (transport.compareTo(BigDecimal.ZERO) > 0)
            totalRow(totals, "Transport", fmt(transport), tlF, tvF, C_WHITE);

        if (extra.compareTo(BigDecimal.ZERO) > 0)
            totalRow(totals, "Others", fmt(extra), tlF, tvF, C_WHITE);

        totalRow(totals, "Round Off", fmt(roundOff), tlF, regular(8, C_MUTED), C_WHITE);

        if (discount.compareTo(BigDecimal.ZERO) > 0)
            totalRow(totals, "Discount", "-" + fmt(discount), tlF, regular(8, C_GREEN), C_WHITE);

        // Grand Total light row
        PdfPCell gtL = new PdfPCell(new Phrase("Grand Total", bold(9, C_BLACK)));
        gtL.setBackgroundColor(new Color(240, 240, 240));
        gtL.setBorder(Rectangle.BOX);
        gtL.setBorderColor(C_ACCENT);
        gtL.setBorderWidth(1.2f);
        gtL.setPadding(7);
        gtL.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell gtR = new PdfPCell(new Phrase(fmt(rounded), bold(10, C_BLACK)));
        gtR.setBackgroundColor(new Color(240, 240, 240));
        gtR.setBorder(Rectangle.BOX);
        gtR.setBorderColor(C_ACCENT);
        gtR.setBorderWidth(1.2f);
        gtR.setPadding(7);
        gtR.setHorizontalAlignment(Element.ALIGN_RIGHT);

        totals.addCell(gtL);
        totals.addCell(gtR);
        right.addElement(totals);

        t.addCell(left);
        t.addCell(right);
        doc.add(t);
    }

    // ─────────────────────────────────────────────────────
    //  SECTION 5 — Amount in Words
    // ─────────────────────────────────────────────────────
    private void addAmountInWords(Document doc, Bill bill) throws Exception {
        BigDecimal total = orZero(bill.getTotalAmount())
                .setScale(2, RoundingMode.HALF_UP);

        long rupees = total.longValue();
        int  paise  = total.remainder(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        StringBuilder words = new StringBuilder("Rupees: ");
        words.append(numberToWords(rupees));
        if (paise > 0) {
            words.append(" And Paise ").append(numberToWords(paise));
        }
        words.append(" Only");

        String finalWords = toTitleCase(words.toString().trim());

        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(0);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(C_BORDER);
        cell.setBorderWidth(0.8f);
        cell.setBackgroundColor(C_LIGHT_BLUE);
        cell.setPaddingTop(6);
        cell.setPaddingBottom(6);
        cell.setPaddingLeft(8);
        cell.setPaddingRight(8);

        Paragraph p = new Paragraph();
        p.add(new Chunk("Amount in Words:  ", bold(7, C_MUTED)));
        p.add(new Chunk(finalWords,           bold(8, C_ACCENT)));
        cell.addElement(p);

        t.addCell(cell);
        doc.add(t);
    }

    // ─────────────────────────────────────────────────────
    //  SECTION 6 — Terms & Signature
    // ─────────────────────────────────────────────────────
    private void addTermsAndSignature(Document doc) throws Exception {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{60f, 40f});
        t.setSpacingBefore(0);

        // Left: Terms
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.BOX);
        left.setBorderColor(C_BORDER);
        left.setBorderWidth(0.8f);
        left.setPadding(7);
        left.setMinimumHeight(80f);

        Font tf = regular(7, new Color(70, 70, 80));
        Font tb = bold(7, C_ACCENT);

        String termsText = shopConfig.getTerms();
        if (termsText != null && !termsText.isBlank()) {
            for (String line : termsText.split("\n")) {
                left.addElement(new Paragraph(line.trim(), tf));
            }
        } else {
            left.addElement(new Paragraph("--Interest @24% will be charged on the bill if not paid on the due date.", tf));
            left.addElement(new Paragraph("--Goods are sent at owner's risk and our responsibility ceases on the goods leaving our premises.", tf));
            left.addElement(new Paragraph("--Goods once sold will not be taken back.", tf));
            left.addElement(spacer(4));
            left.addElement(new Paragraph("-- SUBJECT TO LOCAL JURISDICTION", tf));
            left.addElement(new Paragraph("-- CHEQUE RETURN CHARGES RS. 300/-", tf));
        }
        
        left.addElement(spacer(6));
        
        if (shopConfig.getThankYouMsg() != null && !shopConfig.getThankYouMsg().isBlank()) {
            left.addElement(new Paragraph(shopConfig.getThankYouMsg(), bold(8, C_BLACK)));
            left.addElement(spacer(2));
        }
        left.addElement(spacer(4));
        left.addElement(new Paragraph("Firm GST No : " + shopConfig.getGstin(), tb));
        left.addElement(new Paragraph("FSSAI No: "     + shopConfig.getFssaiNo(), tb));

        // Right: Signature
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.BOX);
        right.setBorderColor(C_BORDER);
        right.setBorderWidth(0.8f);
        right.setPadding(7);
        right.setMinimumHeight(80f);

        Paragraph forLine = new Paragraph("For " + shopConfig.getName(), bold(9, C_DARK_NAVY));
        forLine.setAlignment(Element.ALIGN_CENTER);
        right.addElement(forLine);
        right.addElement(spacer(30));

        Paragraph receiver = new Paragraph("Receiver's Name, Stamp & Signature", regular(7, C_MUTED));
        receiver.setAlignment(Element.ALIGN_CENTER);
        right.addElement(receiver);

        t.addCell(left);
        t.addCell(right);
        doc.add(t);
    }

    // ═══════════════════════════════════════════════════════
    //  FONT HELPERS
    // ═══════════════════════════════════════════════════════

    private Font regular(float size, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, color);
    }

    private Font bold(float size, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, size, color);
    }

    // ═══════════════════════════════════════════════════════
    //  CELL / TABLE HELPERS
    // ═══════════════════════════════════════════════════════

    private Paragraph spacer(float pt) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingBefore(pt);
        p.setSpacingAfter(0);
        return p;
    }

    private void addLabelValue(PdfPCell cell, String label, String value, float size) {
        if (value == null || value.isBlank()) return;
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", regular(size - 1, C_MUTED)));
        p.add(new Chunk(value,       bold(size, C_BLACK)));
        p.setSpacingAfter(2);
        cell.addElement(p);
    }

    private PdfPCell thCell(String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(C_TABLE_HEAD);
        c.setBorderColor(C_BORDER);
        c.setBorderWidth(0.8f);
        c.setPaddingTop(5);
        c.setPaddingBottom(5);
        c.setPaddingLeft(4);
        c.setPaddingRight(4);
        c.setHorizontalAlignment(align);
        return c;
    }

    private void th(PdfPTable t, String text, Font font,
                    int align, int rowspan, int colspan) {
        PdfPCell c = thCell(text, font, align);
        if (rowspan > 1) c.setRowspan(rowspan);
        if (colspan > 1) c.setColspan(colspan);
        t.addCell(c);
    }

    private void td(PdfPTable t, String text, Font font, int align, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(bg);
        c.setBorderColor(C_BORDER);
        c.setBorderWidth(0.4f);
        c.setPaddingTop(4);
        c.setPaddingBottom(4);
        c.setPaddingLeft(4);
        c.setPaddingRight(4);
        c.setHorizontalAlignment(align);
        t.addCell(c);
    }

    private void miniTh(PdfPTable t, String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(new Color(215, 215, 215));
        c.setBorderColor(C_BORDER);
        c.setBorderWidth(0.5f);
        c.setPadding(3);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(c);
    }

    private void miniTd(PdfPTable t, String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(C_GST_BG);
        c.setBorderColor(C_BORDER);
        c.setBorderWidth(0.4f);
        c.setPadding(3);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(c);
    }

    private void totalRow(PdfPTable t, String label, String value,
                          Font lFont, Font vFont, Color bg) {
        PdfPCell lc = new PdfPCell(new Phrase(label, lFont));
        lc.setBackgroundColor(bg);
        lc.setBorderColor(C_BORDER);
        lc.setBorderWidth(0.4f);
        lc.setPadding(5);
        lc.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell vc = new PdfPCell(new Phrase(value, vFont));
        vc.setBackgroundColor(bg);
        vc.setBorderColor(C_BORDER);
        vc.setBorderWidth(0.4f);
        vc.setPadding(5);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);

        t.addCell(lc);
        t.addCell(vc);
    }

    // ═══════════════════════════════════════════════════════
    //  UTILITY
    // ═══════════════════════════════════════════════════════

    private String fmt(BigDecimal v) {
        if (v == null) return "0.00";
        return String.format("%.2f", v);
    }

    private BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    // ── Number to Words (Indian system) ──────────────────
    // ONES and TENS are defined as class-level static fields above.
    // This method is the ONLY numberToWords() in the class.
    public String numberToWords(long n) {
        if (n == 0) return "Zero";
        if (n < 0)  return "Minus " + numberToWords(-n);

        StringBuilder result = new StringBuilder();

        if (n >= 10_000_000L) {
            result.append(numberToWords(n / 10_000_000L)).append(" Crore ");
            n %= 10_000_000L;
        }
        if (n >= 100_000L) {
            result.append(numberToWords(n / 100_000L)).append(" Lakh ");
            n %= 100_000L;
        }
        if (n >= 1_000L) {
            result.append(numberToWords(n / 1_000L)).append(" Thousand ");
            n %= 1_000L;
        }
        if (n >= 100L) {
            result.append(ONES[(int)(n / 100)]).append(" Hundred ");
            n %= 100L;
        }
        if (n >= 20L) {
            result.append(TENS[(int)(n / 10)]).append(" ");
            n %= 10L;
        }
        if (n > 0L) {
            result.append(ONES[(int) n]).append(" ");
        }

        return result.toString().trim();
    }
    // ✅ Add this STATIC version alongside your existing instance method
// The receipt HTML calls this via T() expression
    public static String numberToWordsStatic(long n) {
        if (n == 0) return "Zero";
        if (n < 0)  return "Minus " + numberToWordsStatic(-n);

        final String[] ONES_S = {
                "", "One", "Two", "Three", "Four", "Five", "Six", "Seven",
                "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen",
                "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
        };
        final String[] TENS_S = {
                "", "", "Twenty", "Thirty", "Forty", "Fifty",
                "Sixty", "Seventy", "Eighty", "Ninety"
        };

        StringBuilder result = new StringBuilder();
        if (n >= 10_000_000L) {
            result.append(numberToWordsStatic(n / 10_000_000L))
                    .append(" Crore ");
            n %= 10_000_000L;
        }
        if (n >= 100_000L) {
            result.append(numberToWordsStatic(n / 100_000L))
                    .append(" Lakh ");
            n %= 100_000L;
        }
        if (n >= 1_000L) {
            result.append(numberToWordsStatic(n / 1_000L))
                    .append(" Thousand ");
            n %= 1_000L;
        }
        if (n >= 100L) {
            result.append(ONES_S[(int)(n / 100)])
                    .append(" Hundred ");
            n %= 100L;
        }
        if (n >= 20L) {
            result.append(TENS_S[(int)(n / 10)]).append(" ");
            n %= 10L;
        }
        if (n > 0L) {
            result.append(ONES_S[(int) n]).append(" ");
        }
        return result.toString().trim();
    }
}