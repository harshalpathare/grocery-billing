package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Customer;
import com.example.grocery_billing.entity.Supplier;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelCreditReportService {

    private final CustomerService customerService;
    private final SupplierService supplierService;

    public void exportDuesReport(OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle amountStyle = createAmountStyle(workbook);

            List<Customer> customers = customerService.getCustomersWithPendingBalance();
            createCustomerSheet(workbook, customers, headerStyle, amountStyle);

            List<Supplier> suppliers = supplierService.getSuppliersWithPendingBalance();
            createSupplierSheet(workbook, suppliers, headerStyle, amountStyle);

            workbook.write(outputStream);
        }
    }

    private void createCustomerSheet(Workbook workbook, List<Customer> customers, CellStyle headerStyle, CellStyle amountStyle) {
        Sheet sheet = workbook.createSheet("Customer Dues");
        Row headerRow = sheet.createRow(0);
        String[] columns = {"Customer Name", "Phone", "Total Credit", "Total Paid", "Pending Balance"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        BigDecimal grandTotalCredit = BigDecimal.ZERO;
        BigDecimal grandTotalPaid = BigDecimal.ZERO;
        BigDecimal grandTotalPending = BigDecimal.ZERO;

        for (Customer c : customers) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(c.getName() != null ? c.getName() : "");
            row.createCell(1).setCellValue(c.getPhone() != null ? c.getPhone() : "");

            BigDecimal credit = c.getTotalCredit() != null ? c.getTotalCredit() : BigDecimal.ZERO;
            Cell creditCell = row.createCell(2);
            creditCell.setCellValue(credit.doubleValue());
            creditCell.setCellStyle(amountStyle);

            BigDecimal paid = c.getTotalPaid() != null ? c.getTotalPaid() : BigDecimal.ZERO;
            Cell paidCell = row.createCell(3);
            paidCell.setCellValue(paid.doubleValue());
            paidCell.setCellStyle(amountStyle);

            BigDecimal pending = credit.subtract(paid);
            Cell pendingCell = row.createCell(4);
            pendingCell.setCellValue(pending.doubleValue());
            pendingCell.setCellStyle(amountStyle);

            grandTotalCredit = grandTotalCredit.add(credit);
            grandTotalPaid = grandTotalPaid.add(paid);
            grandTotalPending = grandTotalPending.add(pending);
        }

        Row summaryRow = sheet.createRow(rowIdx + 1);
        Cell summaryLabelCell = summaryRow.createCell(1);
        summaryLabelCell.setCellValue("Grand Total:");
        summaryLabelCell.setCellStyle(headerStyle);

        summaryRow.createCell(2).setCellValue(grandTotalCredit.doubleValue());
        summaryRow.getCell(2).setCellStyle(amountStyle);

        summaryRow.createCell(3).setCellValue(grandTotalPaid.doubleValue());
        summaryRow.getCell(3).setCellStyle(amountStyle);

        summaryRow.createCell(4).setCellValue(grandTotalPending.doubleValue());
        summaryRow.getCell(4).setCellStyle(amountStyle);

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createSupplierSheet(Workbook workbook, List<Supplier> suppliers, CellStyle headerStyle, CellStyle amountStyle) {
        Sheet sheet = workbook.createSheet("Supplier Dues");
        Row headerRow = sheet.createRow(0);
        String[] columns = {"Supplier Name", "Phone", "Total Payable", "Total Paid", "Pending Balance"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        BigDecimal grandTotalPayable = BigDecimal.ZERO;
        BigDecimal grandTotalPaid = BigDecimal.ZERO;
        BigDecimal grandTotalPending = BigDecimal.ZERO;

        for (Supplier s : suppliers) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(s.getName() != null ? s.getName() : "");
            row.createCell(1).setCellValue(s.getPhone() != null ? s.getPhone() : "");

            BigDecimal payable = s.getTotalPayable() != null ? s.getTotalPayable() : BigDecimal.ZERO;
            Cell payableCell = row.createCell(2);
            payableCell.setCellValue(payable.doubleValue());
            payableCell.setCellStyle(amountStyle);

            BigDecimal paid = s.getTotalPaid() != null ? s.getTotalPaid() : BigDecimal.ZERO;
            Cell paidCell = row.createCell(3);
            paidCell.setCellValue(paid.doubleValue());
            paidCell.setCellStyle(amountStyle);

            BigDecimal pending = payable.subtract(paid);
            Cell pendingCell = row.createCell(4);
            pendingCell.setCellValue(pending.doubleValue());
            pendingCell.setCellStyle(amountStyle);

            grandTotalPayable = grandTotalPayable.add(payable);
            grandTotalPaid = grandTotalPaid.add(paid);
            grandTotalPending = grandTotalPending.add(pending);
        }

        Row summaryRow = sheet.createRow(rowIdx + 1);
        Cell summaryLabelCell = summaryRow.createCell(1);
        summaryLabelCell.setCellValue("Grand Total:");
        summaryLabelCell.setCellStyle(headerStyle);

        summaryRow.createCell(2).setCellValue(grandTotalPayable.doubleValue());
        summaryRow.getCell(2).setCellStyle(amountStyle);

        summaryRow.createCell(3).setCellValue(grandTotalPaid.doubleValue());
        summaryRow.getCell(3).setCellStyle(amountStyle);

        summaryRow.createCell(4).setCellValue(grandTotalPending.doubleValue());
        summaryRow.getCell(4).setCellStyle(amountStyle);

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createAmountStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));
        return style;
    }
}
