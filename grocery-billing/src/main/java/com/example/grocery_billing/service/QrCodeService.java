package com.example.grocery_billing.service;

import com.example.grocery_billing.config.ShopConfig;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * QR CODE SERVICE
 *
 * Generates UPI payment QR codes.
 *
 * UPI deep link format:
 *   upi://pay?pa=VPA&pn=NAME&am=AMOUNT&cu=INR&tn=NOTE
 *
 *   pa = Payee UPI VPA (e.g. shop@upi)
 *   pn = Payee Name (e.g. My Grocery Store)
 *   am = Amount (e.g. 500.00)
 *   cu = Currency (always INR)
 *   tn = Transaction Note (e.g. Bill BILL-2026-0001)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeService {

    private final ShopConfig shopConfig;

    // ─────────────────────────────────────────────────────
    // GENERATE UPI QR — returns Base64 encoded PNG image
    // ─────────────────────────────────────────────────────
    /**
     * Generates a UPI payment QR code for a bill.
     *
     * @param amount   Bill total amount
     * @param billNo   Bill number (used as transaction note)
     * @param size     QR image size in pixels (e.g. 200)
     * @return Base64 encoded PNG string — use directly in <img src="data:image/png;base64,...">
     */
    public String generateUpiQr(BigDecimal amount,
                                String billNo,
                                int size) {
        try {
            // Check if UPI ID is configured
            if (shopConfig.getUpiId() == null
                    || shopConfig.getUpiId().isBlank()) {
                log.warn("UPI ID not configured in application.properties");
                return null;
            }

            // Build UPI deep link string
            String upiString = buildUpiString(amount, billNo);
            log.info("Generated UPI string: {}", upiString);

            // Generate QR code
            return generateQrBase64(upiString, size);

        } catch (Exception e) {
            log.error("Error generating UPI QR code", e);
            return null;
        }
    }

    // ─────────────────────────────────────────────────────
    // BUILD UPI DEEP LINK STRING
    // ─────────────────────────────────────────────────────
    private String buildUpiString(BigDecimal amount, String billNo) {
        try {
            String shopName = URLEncoder.encode(
                    shopConfig.getName(), StandardCharsets.UTF_8);
            String note     = URLEncoder.encode(
                    "Bill " + billNo, StandardCharsets.UTF_8);
            String amtStr   = String.format("%.2f", amount);

            return "upi://pay"
                    + "?pa=" + shopConfig.getUpiId()
                    + "&pn=" + shopName
                    + "&am=" + amtStr
                    + "&cu=INR"
                    + "&tn=" + note;

        } catch (Exception e) {
            // Fallback without encoding
            return "upi://pay"
                    + "?pa=" + shopConfig.getUpiId()
                    + "&pn=" + shopConfig.getName()
                    + "&am=" + String.format("%.2f", amount)
                    + "&cu=INR"
                    + "&tn=Bill+" + billNo;
        }
    }

    // ─────────────────────────────────────────────────────
    // GENERATE QR AND CONVERT TO BASE64
    // ─────────────────────────────────────────────────────
    private String generateQrBase64(String content, int size)
            throws WriterException, Exception {

        // QR code generation hints
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);  // quiet zone

        // Generate bit matrix
        QRCodeWriter writer   = new QRCodeWriter();
        BitMatrix    matrix   = writer.encode(
                content, BarcodeFormat.QR_CODE, size, size, hints);

        // Convert to image
        // Black QR on white background
        MatrixToImageConfig config = new MatrixToImageConfig(
                0xFF000000,  // black modules
                0xFFFFFFFF   // white background
        );
        BufferedImage image = MatrixToImageWriter.toBufferedImage(
                matrix, config);

        // Convert image to Base64 PNG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] imageBytes = baos.toByteArray();

        return Base64.getEncoder().encodeToString(imageBytes);
    }

    // ─────────────────────────────────────────────────────
    // GENERATE QR FOR RECEIPT (smaller size — 150px)
    // ─────────────────────────────────────────────────────
    public String generateUpiQrForReceipt(BigDecimal amount,
                                          String billNo) {
        return generateUpiQr(amount, billNo, 150);
    }

    // ─────────────────────────────────────────────────────
    // GENERATE QR FOR BILL VIEW (larger — 200px)
    // ─────────────────────────────────────────────────────
    public String generateUpiQrForView(BigDecimal amount,
                                       String billNo) {
        return generateUpiQr(amount, billNo, 200);
    }

    // ─────────────────────────────────────────────────────
    // CHECK IF UPI IS CONFIGURED
    // ─────────────────────────────────────────────────────
    public boolean isUpiConfigured() {
        return shopConfig.getUpiId() != null
                && !shopConfig.getUpiId().isBlank();
    }
}