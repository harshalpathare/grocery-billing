package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.CashFlow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelCashFlowService {

    private static final String[] HEADERS = {
            "Date", "Type", "Category", "Description", "Amount"
    };

    public void exportToExcel(List<CashFlow> cashFlows, OutputStream os) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Cash Flow Ledger");

            // Header Row
            Row headerRow = sheet.createRow(0);
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
            int rowIdx = 1;
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            
            for (CashFlow cf : cashFlows) {
                Row row = sheet.createRow(rowIdx++);

                // Date
                String dateStr = cf.getTransactionDate() != null ? cf.getTransactionDate().format(dateFormatter) : "";
                row.createCell(0).setCellValue(dateStr);

                // Type
                row.createCell(1).setCellValue(cf.getType() != null ? cf.getType().name() : "");

                // Category
                row.createCell(2).setCellValue(cf.getCategory() != null ? cf.getCategory() : "");

                // Description
                row.createCell(3).setCellValue(cf.getDescription() != null ? cf.getDescription() : "");

                // Amount
                row.createCell(4).setCellValue(cf.getAmount() != null ? cf.getAmount().doubleValue() : 0.0);
            }

            // Auto-size columns
            for (int col = 0; col < HEADERS.length; col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(os);
        }
    }
}
