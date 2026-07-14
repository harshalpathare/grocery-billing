package com.example.grocery_billing.service;

import com.example.grocery_billing.dto.JournalEntryDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ExcelAccountingService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    public void exportJournal(List<JournalEntryDto> entries, OutputStream out) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Journal");
            createHeader(sheet, "Date", "Reference", "Account", "Narration", "Debit (₹)", "Credit (₹)");

            int rowIdx = 1;
            BigDecimal totalDr = BigDecimal.ZERO;
            BigDecimal totalCr = BigDecimal.ZERO;

            for (JournalEntryDto e : entries) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(e.getDate() != null ? e.getDate().format(DATE_FMT) : "");
                row.createCell(1).setCellValue(e.getReference());
                row.createCell(2).setCellValue(e.getAccount());
                row.createCell(3).setCellValue(e.getNarration());
                
                if (e.getDebit() != null) {
                    row.createCell(4).setCellValue(e.getDebit().doubleValue());
                    totalDr = totalDr.add(e.getDebit());
                }
                if (e.getCredit() != null) {
                    row.createCell(5).setCellValue(e.getCredit().doubleValue());
                    totalCr = totalCr.add(e.getCredit());
                }
            }

            // Totals row
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.createCell(3).setCellValue("TOTAL");
            totalRow.createCell(4).setCellValue(totalDr.doubleValue());
            totalRow.createCell(5).setCellValue(totalCr.doubleValue());

            autoSize(sheet, 6);
            wb.write(out);
        }
    }

    public void exportLedger(String accountName, List<JournalEntryDto> entries, OutputStream out) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Ledger - " + accountName.replaceAll("[^a-zA-Z0-9 ]", ""));
            createHeader(sheet, "Date", "Reference", "Narration", "Debit (₹)", "Credit (₹)", "Balance (₹)");

            int rowIdx = 1;
            BigDecimal balance = BigDecimal.ZERO;

            for (JournalEntryDto e : entries) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(e.getDate() != null ? e.getDate().format(DATE_FMT) : "");
                row.createCell(1).setCellValue(e.getReference());
                row.createCell(2).setCellValue(e.getNarration());
                
                BigDecimal dr = e.getDebit() != null ? e.getDebit() : BigDecimal.ZERO;
                BigDecimal cr = e.getCredit() != null ? e.getCredit() : BigDecimal.ZERO;
                
                if (dr.compareTo(BigDecimal.ZERO) > 0) row.createCell(3).setCellValue(dr.doubleValue());
                if (cr.compareTo(BigDecimal.ZERO) > 0) row.createCell(4).setCellValue(cr.doubleValue());
                
                balance = balance.add(dr).subtract(cr);
                row.createCell(5).setCellValue(balance.doubleValue());
            }

            autoSize(sheet, 6);
            wb.write(out);
        }
    }

    public void exportTrialBalance(Map<String, BigDecimal> tb, OutputStream out) throws Exception {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Trial Balance");
            createHeader(sheet, "Account Name", "Debit Balance (₹)", "Credit Balance (₹)");

            int rowIdx = 1;
            BigDecimal totalDr = BigDecimal.ZERO;
            BigDecimal totalCr = BigDecimal.ZERO;

            for (Map.Entry<String, BigDecimal> e : tb.entrySet()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(e.getKey());
                
                BigDecimal bal = e.getValue();
                if (bal.compareTo(BigDecimal.ZERO) > 0) {
                    row.createCell(1).setCellValue(bal.doubleValue());
                    totalDr = totalDr.add(bal);
                } else if (bal.compareTo(BigDecimal.ZERO) < 0) {
                    row.createCell(2).setCellValue(bal.abs().doubleValue());
                    totalCr = totalCr.add(bal.abs());
                }
            }

            Row totalRow = sheet.createRow(rowIdx);
            totalRow.createCell(0).setCellValue("TOTAL");
            totalRow.createCell(1).setCellValue(totalDr.doubleValue());
            totalRow.createCell(2).setCellValue(totalCr.doubleValue());

            autoSize(sheet, 3);
            wb.write(out);
        }
    }

    private void createHeader(Sheet sheet, String... headers) {
        Row row = sheet.createRow(0);
        Workbook wb = sheet.getWorkbook();
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
