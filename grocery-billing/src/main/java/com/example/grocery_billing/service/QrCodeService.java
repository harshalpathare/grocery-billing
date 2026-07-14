package com.example.grocery_billing.service;

import com.example.grocery_billing.config.ShopContext;
import com.example.grocery_billing.entity.Shop;
import com.example.grocery_billing.repository.ShopRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeService {

    private final ShopRepository shopRepository;
    private final ShopConfig     shopConfig; // fallback

    // ── Resolve effective UPI id and shop name ────────────
    private String effectiveUpiId() {
        Long shopId = ShopContext.getShopId();
        if (shopId != null) {
            return shopRepository.findById(shopId)
                    .map(Shop::getUpiId)
                    .filter(u -> u != null && !u.isBlank())
                    .orElse(shopConfig.getUpiId());
        }
        return shopConfig.getUpiId();
    }

    private String effectiveShopName() {
        Long shopId = ShopContext.getShopId();
        if (shopId != null) {
            return shopRepository.findById(shopId)
                    .map(Shop::getShopName)
                    .filter(n -> n != null && !n.isBlank())
                    .orElse(shopConfig.getName());
        }
        return shopConfig.getName();
    }

    // ─────────────────────────────────────────────────────
    // GENERATE UPI QR
    // ─────────────────────────────────────────────────────
    public String generateUpiQr(BigDecimal amount, String billNo, int size) {
        try {
            String upiId = effectiveUpiId();
            if (upiId == null || upiId.isBlank()) {
                log.warn("UPI ID not configured");
                return null;
            }
            String upiString = buildUpiString(amount, billNo, upiId);
            return generateQrBase64(upiString, size);
        } catch (Exception e) {
            log.error("Error generating UPI QR", e);
            return null;
        }
    }

    private String buildUpiString(BigDecimal amount, String billNo, String upiId) {
        try {
            String shopName = URLEncoder.encode(effectiveShopName(), StandardCharsets.UTF_8);
            String note     = URLEncoder.encode("Bill " + billNo, StandardCharsets.UTF_8);
            String amtStr   = String.format("%.2f", amount);
            return "upi://pay?pa=" + upiId + "&pn=" + shopName
                    + "&am=" + amtStr + "&cu=INR&tn=" + note;
        } catch (Exception e) {
            return "upi://pay?pa=" + upiId + "&pn=" + effectiveShopName()
                    + "&am=" + String.format("%.2f", amount) + "&cu=INR&tn=Bill+" + billNo;
        }
    }

    private String generateQrBase64(String content, int size) throws WriterException, Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix    matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

        MatrixToImageConfig config = new MatrixToImageConfig(0xFF000000, 0xFFFFFFFF);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix, config);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public String generateUpiQrForReceipt(BigDecimal amount, String billNo) {
        return generateUpiQr(amount, billNo, 150);
    }

    public String generateUpiQrForView(BigDecimal amount, String billNo) {
        return generateUpiQr(amount, billNo, 200);
    }

    public boolean isUpiConfigured() {
        String upiId = effectiveUpiId();
        return upiId != null && !upiId.isBlank();
    }
}
