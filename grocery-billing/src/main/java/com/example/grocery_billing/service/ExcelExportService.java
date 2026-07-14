package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Bill;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ExcelExportService {

    public byte[] generateDailyExcel(List<Bill> bills, String dateStr) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Daily Sales " + dateStr);
            createHeader(workbook, sheet, "Bill No", "Date", "Customer", "Total Amount");

            int rowNum = 1;
            for (Bill b : bills) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(b.getBillNo());
                row.createCell(1).setCellValue(b.getBillDate().toString());
                row.createCell(2).setCellValue(b.getCustomer() != null ? b.getCustomer().getName() : "Walk-in");
                row.createCell(3).setCellValue(b.getTotalAmount() != null ? b.getTotalAmount().doubleValue() : 0.0);
            }

            for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateMonthlyExcel(List<ReportService.MonthlyRow> rows, int year) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Monthly Sales " + year);
            createHeader(workbook, sheet, "Month", "Bills Count", "Sales (Rs)", "GST Collected (Rs)");

            int rowNum = 1;
            for (ReportService.MonthlyRow r : rows) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.monthName());
                row.createCell(1).setCellValue(r.billCount());
                row.createCell(2).setCellValue(r.sales() != null ? r.sales().doubleValue() : 0.0);
                row.createCell(3).setCellValue(r.gst() != null ? r.gst().doubleValue() : 0.0);
            }

            for (int i = 0; i < 4; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateGstExcel(List<Bill> bills, String start, String end) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("GST Report");
            createHeader(workbook, sheet, "Bill No", "Date", "Customer", "Taxable Amount", "CGST", "SGST", "Total GST", "Invoice Total");

            int rowNum = 1;
            for (Bill b : bills) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(b.getBillNo());
                row.createCell(1).setCellValue(b.getBillDate().toString());
                row.createCell(2).setCellValue(b.getCustomer() != null ? b.getCustomer().getName() : "Walk-in");
                
                BigDecimal total = b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal gstAmt = b.getGstAmount() != null ? b.getGstAmount() : BigDecimal.ZERO;
                BigDecimal taxable = total.subtract(gstAmt);
                BigDecimal halfGst = gstAmt.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

                row.createCell(3).setCellValue(taxable.doubleValue());
                row.createCell(4).setCellValue(halfGst.doubleValue());
                row.createCell(5).setCellValue(halfGst.doubleValue());
                row.createCell(6).setCellValue(gstAmt.doubleValue());
                row.createCell(7).setCellValue(total.doubleValue());
            }

            for (int i = 0; i < 8; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateAllBillsExcel(List<Bill> bills, String start, String end) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("All Bills");
            createHeader(workbook, sheet, "Bill No", "Date", "Time", "Customer", "Payment Mode", "Status", "Total Items", "Discount", "GST", "Total Amount");

            int rowNum = 1;
            for (Bill b : bills) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(b.getBillNo());
                row.createCell(1).setCellValue(b.getBillDate().toString());
                row.createCell(2).setCellValue(b.getCreatedAt() != null ? b.getCreatedAt().toLocalTime().toString().substring(0, 5) : "");
                row.createCell(3).setCellValue(b.getCustomer() != null ? b.getCustomer().getName() : "Walk-in");
                row.createCell(4).setCellValue(b.getPaymentMethod() != null ? b.getPaymentMethod() : "");
                row.createCell(5).setCellValue(b.getPaymentStatus() != null ? b.getPaymentStatus().name() : "");
                row.createCell(6).setCellValue(b.getBillItems() != null ? b.getBillItems().size() : 0);
                row.createCell(7).setCellValue(b.getDiscount() != null ? b.getDiscount().doubleValue() : 0.0);
                row.createCell(8).setCellValue(b.getGstAmount() != null ? b.getGstAmount().doubleValue() : 0.0);
                row.createCell(9).setCellValue(b.getTotalAmount() != null ? b.getTotalAmount().doubleValue() : 0.0);
            }

            for (int i = 0; i < 10; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createHeader(Workbook workbook, Sheet sheet, String... columns) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }
    }
}
