package com.example.grocery_billing.controller;



import com.example.grocery_billing.entity.Bill;
import com.example.grocery_billing.service.BillService;
import com.example.grocery_billing.service.PdfInvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * PDF CONTROLLER
 *
 * Handles PDF download requests.
 *
 * When user clicks "Download PDF":
 *  1. We fetch the bill from DB
 *  2. Pass it to PdfInvoiceService to generate bytes
 *  3. Return those bytes as a downloadable PDF file
 *
 * @ResponseBody is NOT needed here because we return
 * ResponseEntity<byte[]> directly — Spring handles it.
 */
@Controller
@RequestMapping("/bills/pdf")
@RequiredArgsConstructor
@Slf4j
public class PdfController {

    private final BillService       billService;
    private final PdfInvoiceService pdfInvoiceService;

    // ─────────────────────────────────────────────────────
    // DOWNLOAD PDF
    // GET /bills/pdf/{id}
    // ─────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id) {
        try {
            // 1. Load the bill
            Bill bill = billService.getBillById(id);

            // 2. Generate PDF bytes
            byte[] pdfBytes = pdfInvoiceService.generateInvoicePdf(bill);

            // 3. Set up download headers — smart filename with customer + date
            String customerPart = (bill.getCustomer() != null && bill.getCustomer().getName() != null)
                    ? bill.getCustomer().getName().replaceAll("[^a-zA-Z0-9]", "_")
                    : "WalkIn";
            String datePart = bill.getBillDate() != null
                    ? bill.getBillDate().format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy"))
                    : java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
            String filename = customerPart + "_" + bill.getBillNo() + "_" + datePart + ".pdf";

            HttpHeaders headers = new HttpHeaders();

            // Content-Type: tells browser this is a PDF
            headers.setContentType(MediaType.APPLICATION_PDF);

            // Content-Disposition: "attachment" = download, "inline" = open in browser
            // Change "attachment" to "inline" if you want it to open in browser tab
            headers.setContentDispositionFormData("attachment", filename);

            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            headers.setContentLength(pdfBytes.length);

            log.info("PDF generated for bill: {} ({} bytes)", bill.getBillNo(), pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("Error generating PDF for bill id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────────────
    // OPEN PDF IN BROWSER (preview)
    // GET /bills/pdf/{id}/preview
    // ─────────────────────────────────────────────────────
    @GetMapping("/{id}/preview")
    public ResponseEntity<byte[]> previewInvoice(@PathVariable Long id) {
        try {
            Bill bill = billService.getBillById(id);
            byte[] pdfBytes = pdfInvoiceService.generateInvoicePdf(bill);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // "inline" = opens PDF in browser tab instead of downloading
            headers.add("Content-Disposition",
                    "inline; filename=\"Invoice-" + bill.getBillNo() + ".pdf\"");
            headers.setContentLength(pdfBytes.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("Error previewing PDF for bill id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────────────
    // PRINT PDF IN BROWSER
    // GET /bills/pdf/{id}/print
    // Opens a minimal page that loads the preview PDF and triggers print
    // ─────────────────────────────────────────────────────
    @GetMapping("/{id}/print")
    public String printInvoicePage(@PathVariable Long id, Model model) {
        Bill bill = billService.getBillById(id);
        model.addAttribute("bill", bill);
        return "bill/pdf-print";
    }
}