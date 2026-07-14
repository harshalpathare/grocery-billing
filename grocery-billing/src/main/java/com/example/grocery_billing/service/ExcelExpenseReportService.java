package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Expense;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExcelExpenseReportService {

    private final ExpenseService expenseService;

    public void exportExpenseReport(LocalDate start, LocalDate end, String category, OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle amountStyle = createAmountStyle(workbook);

            List<Expense> expenses = expenseService.getExpensesByCategoryAndDate(category, start, end);

            createDailySheet(workbook, expenses, headerStyle, dateStyle, amountStyle);
            createMonthlySheet(workbook, expenses, headerStyle, amountStyle);
            createCategoryWiseSheet(workbook, expenses, headerStyle, amountStyle);

            workbook.write(outputStream);
        }
    }

    private void createDailySheet(Workbook workbook, List<Expense> expenses, CellStyle headerStyle, CellStyle dateStyle, CellStyle amountStyle) {
        Sheet sheet = workbook.createSheet("Daily Expenses");
        Row headerRow = sheet.createRow(0);
        String[] columns = {"Date", "Category", "Note", "Amount"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Expense e : expenses) {
            Row row = sheet.createRow(rowIdx++);

            Cell dateCell = row.createCell(0);
            if (e.getExpenseDate() != null) {
                dateCell.setCellValue(e.getExpenseDate());
                dateCell.setCellStyle(dateStyle);
            }

            row.createCell(1).setCellValue(e.getCategory() != null ? e.getCategory() : "");
            row.createCell(2).setCellValue(e.getNote() != null ? e.getNote() : "");

            Cell amountCell = row.createCell(3);
            double amt = e.getAmount() != null ? e.getAmount().doubleValue() : 0.0;
            amountCell.setCellValue(amt);
            amountCell.setCellStyle(amountStyle);

            grandTotal = grandTotal.add(e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO);
        }

        Row summaryRow = sheet.createRow(rowIdx + 1);
        Cell summaryLabelCell = summaryRow.createCell(2);
        summaryLabelCell.setCellValue("Grand Total:");
        summaryLabelCell.setCellStyle(headerStyle);

        Cell sumAmountCell = summaryRow.createCell(3);
        sumAmountCell.setCellValue(grandTotal.doubleValue());
        sumAmountCell.setCellStyle(amountStyle);

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createMonthlySheet(Workbook workbook, List<Expense> expenses, CellStyle headerStyle, CellStyle amountStyle) {
        Sheet sheet = workbook.createSheet("Monthly Summary");
        Row headerRow = sheet.createRow(0);
        String[] columns = {"Month", "Total Amount"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        Map<String, BigDecimal> monthlyTotals = expenses.stream()
                .filter(e -> e.getExpenseDate() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getExpenseDate().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO, BigDecimal::add)
                ));

        int rowIdx = 1;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : monthlyTotals.entrySet()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(entry.getKey());

            Cell amountCell = row.createCell(1);
            amountCell.setCellValue(entry.getValue().doubleValue());
            amountCell.setCellStyle(amountStyle);

            grandTotal = grandTotal.add(entry.getValue());
        }

        Row summaryRow = sheet.createRow(rowIdx + 1);
        Cell summaryLabelCell = summaryRow.createCell(0);
        summaryLabelCell.setCellValue("Grand Total:");
        summaryLabelCell.setCellStyle(headerStyle);

        Cell sumAmountCell = summaryRow.createCell(1);
        sumAmountCell.setCellValue(grandTotal.doubleValue());
        sumAmountCell.setCellStyle(amountStyle);

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createCategoryWiseSheet(Workbook workbook, List<Expense> expenses, CellStyle headerStyle, CellStyle amountStyle) {
        Sheet sheet = workbook.createSheet("Category Wise");
        Row headerRow = sheet.createRow(0);
        String[] columns = {"Category", "Total Amount"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        Map<String, BigDecimal> categoryTotals = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory() != null ? e.getCategory() : "Other",
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO, BigDecimal::add)
                ));

        int rowIdx = 1;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(entry.getKey());

            Cell amountCell = row.createCell(1);
            amountCell.setCellValue(entry.getValue().doubleValue());
            amountCell.setCellStyle(amountStyle);

            grandTotal = grandTotal.add(entry.getValue());
        }

        Row summaryRow = sheet.createRow(rowIdx + 1);
        Cell summaryLabelCell = summaryRow.createCell(0);
        summaryLabelCell.setCellValue("Grand Total:");
        summaryLabelCell.setCellStyle(headerStyle);

        Cell sumAmountCell = summaryRow.createCell(1);
        sumAmountCell.setCellValue(grandTotal.doubleValue());
        sumAmountCell.setCellStyle(amountStyle);

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

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));
        return style;
    }

    private CellStyle createAmountStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("#,##0.00"));
        return style;
    }
}
