package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.PurchaseOrder;
import com.example.grocery_billing.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelPurchaseLedgerService {

    private final PurchaseOrderRepository poRepository;

    public void exportPurchaseLedger(OutputStream outputStream) throws IOException {
        List<PurchaseOrder> orders = poRepository.findAll(Sort.by(Sort.Direction.DESC, "orderDate", "id"));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Purchase Ledger");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Date Style
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));

            // Amount Style
            CellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));

            // Headers
            Row headerRow = sheet.createRow(0);
            String[] columns = {
                    "PO Number", "Date", "Type", "Supplier", "GST Type", 
                    "Items Count", "Subtotal", "Tax Amount", "Total Amount", "Payment Status"
            };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            BigDecimal grandTotal = BigDecimal.ZERO;

            for (PurchaseOrder po : orders) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(po.getPoNumber() != null ? po.getPoNumber() : "");
                
                Cell dateCell = row.createCell(1);
                if (po.getOrderDate() != null) {
                    dateCell.setCellValue(po.getOrderDate());
                    dateCell.setCellStyle(dateStyle);
                }
                
                row.createCell(2).setCellValue(po.getType() != null ? po.getType().name() : "");
                row.createCell(3).setCellValue(po.getSupplier() != null ? po.getSupplier().getName() : "Direct");
                row.createCell(4).setCellValue(Boolean.TRUE.equals(po.getIsGst()) ? "GST" : "Non-GST");
                
                row.createCell(5).setCellValue(po.getItems() != null ? po.getItems().size() : 0);

                Cell subtotalCell = row.createCell(6);
                subtotalCell.setCellValue(po.getSubtotal() != null ? po.getSubtotal().doubleValue() : 0.0);
                subtotalCell.setCellStyle(amountStyle);

                Cell taxCell = row.createCell(7);
                taxCell.setCellValue(po.getTaxAmount() != null ? po.getTaxAmount().doubleValue() : 0.0);
                taxCell.setCellStyle(amountStyle);

                Cell totalCell = row.createCell(8);
                double total = po.getTotalAmount() != null ? po.getTotalAmount().doubleValue() : 0.0;
                totalCell.setCellValue(total);
                totalCell.setCellStyle(amountStyle);

                if (po.getType() == PurchaseOrder.OrderType.RETURN) {
                    grandTotal = grandTotal.subtract(po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO);
                } else {
                    grandTotal = grandTotal.add(po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO);
                }

                row.createCell(9).setCellValue(po.getPaymentStatus() != null ? po.getPaymentStatus().name() : "");
            }

            // Summary Row
            Row summaryRow = sheet.createRow(rowIdx + 1);
            Cell summaryLabelCell = summaryRow.createCell(7);
            summaryLabelCell.setCellValue("Net Purchase Value:");
            summaryLabelCell.setCellStyle(headerStyle);

            Cell summaryValueCell = summaryRow.createCell(8);
            summaryValueCell.setCellValue(grandTotal.doubleValue());
            summaryValueCell.setCellStyle(amountStyle);

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
        }
    }
}
