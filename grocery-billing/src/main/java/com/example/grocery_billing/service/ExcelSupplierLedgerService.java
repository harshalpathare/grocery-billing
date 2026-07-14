package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Supplier;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelSupplierLedgerService {

    private static final String[] HEADERS = {
            "Date", "Particulars", "Type", "Amount", "Running Balance"
    };

    public void exportToExcel(Supplier supplier, List<SupplierService.LedgerEntry> entries, OutputStream os) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Supplier Ledger");

            // Title Row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Ledger for: " + supplier.getName() + (supplier.getGstin() != null ? " (GST: " + supplier.getGstin() + ")" : ""));
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Header Row
            Row headerRow = sheet.createRow(2);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int col = 0; col < HEADERS.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(HEADERS[col]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowIdx = 3;
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            
            for (SupplierService.LedgerEntry entry : entries) {
                Row row = sheet.createRow(rowIdx++);

                // Date
                String dateStr = entry.date() != null ? entry.date().format(dateFormatter) : "";
                row.createCell(0).setCellValue(dateStr);

                // Particulars
                row.createCell(1).setCellValue(entry.particulars() != null ? entry.particulars() : "");

                // Type
                row.createCell(2).setCellValue(entry.type() != null ? entry.type() : "");

                // Amount
                row.createCell(3).setCellValue(entry.amount() != null ? entry.amount().doubleValue() : 0.0);
                
                // Balance
                row.createCell(4).setCellValue(entry.balance() != null ? entry.balance().doubleValue() : 0.0);
            }

            // Auto-size columns
            for (int col = 0; col < HEADERS.length; col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(os);
        }
    }
}
