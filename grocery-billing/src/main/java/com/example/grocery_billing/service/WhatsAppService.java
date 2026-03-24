package com.example.grocery_billing.service;

import com.example.grocery_billing.config.ShopConfig;
import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.entity.BillItem;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    private final ShopConfig shopConfig;
    private final PdfInvoiceService pdfInvoiceService;

    @Value("${twilio.whatsapp.from}")
    private String fromNumber;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─────────────────────────────────────────────
    // MAIN METHOD — Send Bill on WhatsApp
    // ─────────────────────────────────────────────
    public void sendBillOnWhatsApp(Bill bill) throws Exception {

        String phone = getCustomerPhone(bill);
        if (phone == null || phone.isBlank()) {
            throw new RuntimeException(
                    "Customer phone number not found. " +
                            "Please add phone number to send WhatsApp.");
        }

        // Step 1: Send template message (bill summary)
        sendTemplateMessage(phone, bill);

        // Step 2: Send plain text with full item details
        sendTextMessage(phone, buildItemsMessage(bill));

        log.info("WhatsApp bill sent to {} for bill {}",
                phone, bill.getBillNo());
    }

    // ─────────────────────────────────────────────
    // STEP 1 — Send Template Message
    // Uses your approved Twilio template
    // Template variables: {{1}} = date, {{2}} = time
    // ─────────────────────────────────────────────
    private void sendTemplateMessage(String phone, Bill bill) {

        String date  = bill.getBillDate().format(DATE_FMT);
        String total = "Rs." + fmt(bill.getTotalAmount());

        Message.creator(
                        new PhoneNumber("whatsapp:+91" + phone),
                        new PhoneNumber(fromNumber),
                        ""  // empty body required when using ContentSid
                )
                .setContentSid("HXb5b62575e6e4ff6129ad7c8efe1f983e")
                .setContentVariables(
                        "{\"1\":\"" + date + "\",\"2\":\"" + total + "\"}"
                )
                .create();

        log.info("Template message sent to {}", phone);
    }

    // ─────────────────────────────────────────────
    // STEP 2 — Send Full Bill Details as Text
    // ─────────────────────────────────────────────
    private String buildItemsMessage(Bill bill) {
        StringBuilder sb = new StringBuilder();

        sb.append("🛒 *").append(shopConfig.getName().toUpperCase())
                .append("*\n\n");

        sb.append("Hello *")
                .append(bill.getCustomer() != null
                        ? bill.getCustomer().getName()
                        : "Customer")
                .append("*! 👋\n\n");

        sb.append("📄 *Bill No:* ").append(bill.getBillNo()).append("\n");
        sb.append("📅 *Date:* ")
                .append(bill.getBillDate().format(DATE_FMT)).append("\n");
        sb.append("💳 *Payment:* ")
                .append(bill.getPaymentMethod()).append("\n\n");

        // Items list
        sb.append("🧾 *Items Purchased:*\n");
        sb.append("─────────────────────\n");
        for (BillItem item : bill.getBillItems()) {
            String name = item.getProductNameSnapshot() != null
                    ? item.getProductNameSnapshot()
                    : item.getProduct().getNameEn();
            String qty = item.getQuantity()
                    .stripTrailingZeros().toPlainString();
            sb.append("• ").append(name)
                    .append(" × ").append(qty)
                    .append(" = ₹").append(fmt(item.getItemTotal()))
                    .append("\n");
        }
        sb.append("─────────────────────\n");

        // Discount / transport
        if (bill.getDiscount() != null
                && bill.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("🏷️ *Discount:* -₹")
                    .append(fmt(bill.getDiscount())).append("\n");
        }
        if (bill.getTransportCost() != null
                && bill.getTransportCost().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("🚚 *Transport:* ₹")
                    .append(fmt(bill.getTransportCost())).append("\n");
        }

        // Total
        BigDecimal total = bill.getTotalAmount() != null
                ? bill.getTotalAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        sb.append("\n💰 *Total: ₹").append(fmt(total)).append("*\n");

        // Payment status
        switch (bill.getPaymentStatus()) {
            case PAID ->
                    sb.append("✅ *Status: PAID*\n");
            case CREDIT ->
                    sb.append("⚠️ *Status: ON CREDIT (Udhari)*\n")
                            .append("Pending: ₹").append(fmt(total)).append("\n");
            case PARTIAL ->
                    sb.append("🔶 *Status: PARTIAL PAYMENT*\n");
        }

        sb.append("\n🙏 Thank you for shopping with us!\n");
        sb.append("📞 ").append(shopConfig.getPhone());

        if (shopConfig.getAddress() != null
                && !shopConfig.getAddress().isBlank()) {
            sb.append("\n📍 ").append(shopConfig.getAddress());
        }

        return sb.toString();
    }

    // ─────────────────────────────────────────────
    // Send plain text message
    // ─────────────────────────────────────────────
    private void sendTextMessage(String phone, String text) {
        Message.creator(
                new PhoneNumber("whatsapp:+91" + phone),
                new PhoneNumber(fromNumber),
                text
        ).create();
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────
    private String getCustomerPhone(Bill bill) {
        if (bill.getCustomer() == null) return null;
        return bill.getCustomer().getPhone();
    }

    private String fmt(BigDecimal v) {
        if (v == null) return "0.00";
        return String.format("%.2f", v);
    }
}