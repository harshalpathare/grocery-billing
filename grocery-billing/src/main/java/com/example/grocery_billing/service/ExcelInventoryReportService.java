package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Product;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelInventoryReportService {

    private final InventoryService inventoryService;

    public void exportInventory(OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            
            // Reusable Styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle amountStyle = createAmountStyle(workbook);
            CellStyle warningStyle = createWarningStyle(workbook);
            CellStyle dangerStyle = createDangerStyle(workbook);

            // 1. All Stock Report
            List<Product> allStock = inventoryService.getAllStock();
            createStockSheet(workbook, "All Stock", allStock, headerStyle, dateStyle, amountStyle, warningStyle, dangerStyle);

            // 2. Dead Stock (90 days)
            List<Product> deadStock = inventoryService.getDeadStock(90);
            createStockSheet(workbook, "Dead Stock (90 Days)", deadStock, headerStyle, dateStyle, amountStyle, warningStyle, dangerStyle);

            // 3. Fast Moving (30 days)
            List<Object[]> fastMoving = inventoryService.getFastMovingProducts(30);
            createSalesSheet(workbook, "Fast Moving (30 Days)", fastMoving, headerStyle, amountStyle);

            // 4. Slow Moving (30 days)
            List<Object[]> slowMoving = inventoryService.getSlowMovingProducts(30);
            createSalesSheet(workbook, "Slow Moving (30 Days)", slowMoving, headerStyle, amountStyle);

            // 5. Expiry Report (30 days)
            List<Product> expiryStock = inventoryService.getExpiringProducts(30);
            createStockSheet(workbook, "Expiry Report (30 Days)", expiryStock, headerStyle, dateStyle, amountStyle, warningStyle, dangerStyle);

            workbook.write(outputStream);
        }
    }

    private void createStockSheet(Workbook workbook, String sheetName, List<Product> products,
                                  CellStyle headerStyle, CellStyle dateStyle, CellStyle amountStyle, 
                                  CellStyle warningStyle, CellStyle dangerStyle) {
        Sheet sheet = workbook.createSheet(sheetName);

        Row headerRow = sheet.createRow(0);
        String[] columns = {
                "Product Name", "Category", "Barcode / SKU", "Stock Qty", "Unit", 
                "Min Stock", "Cost Price", "Sell Price", "Total Cost Value", 
                "Total Retail Value", "Expiry Date", "Status"
        };

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalRetailValue = BigDecimal.ZERO;

        for (Product p : products) {
            Row row = sheet.createRow(rowIdx++);

            row.createCell(0).setCellValue(p.getNameEn() != null ? p.getNameEn() : "");
            row.createCell(1).setCellValue(p.getCategory() != null ? p.getCategory() : "");
            
            String identifier = (p.getBarcode() != null ? p.getBarcode() : "") + 
                                (p.getSku() != null ? " / " + p.getSku() : "");
            if (identifier.startsWith(" / ")) identifier = identifier.substring(3);
            row.createCell(2).setCellValue(identifier);

            Cell stockCell = row.createCell(3);
            double stock = p.getStockQty() != null ? p.getStockQty().doubleValue() : 0.0;
            stockCell.setCellValue(stock);
            
            row.createCell(4).setCellValue(p.getUnit() != null ? p.getUnit() : "piece");

            Cell minStockCell = row.createCell(5);
            double minStock = p.getMinStock() != null ? p.getMinStock().doubleValue() : 0.0;
            minStockCell.setCellValue(minStock);

            Cell costPriceCell = row.createCell(6);
            double costPrice = p.getCostPrice() != null ? p.getCostPrice().doubleValue() : (p.getPrice() != null ? p.getPrice().doubleValue() : 0.0);
            costPriceCell.setCellValue(costPrice);
            costPriceCell.setCellStyle(amountStyle);

            Cell sellPriceCell = row.createCell(7);
            double sellPrice = p.getPrice() != null ? p.getPrice().doubleValue() : 0.0;
            sellPriceCell.setCellValue(sellPrice);
            sellPriceCell.setCellStyle(amountStyle);

            Cell totalCostCell = row.createCell(8);
            double tCost = stock * costPrice;
            totalCostCell.setCellValue(tCost);
            totalCostCell.setCellStyle(amountStyle);
            totalCostValue = totalCostValue.add(BigDecimal.valueOf(tCost));

            Cell totalRetailCell = row.createCell(9);
            double tRetail = stock * sellPrice;
            totalRetailCell.setCellValue(tRetail);
            totalRetailCell.setCellStyle(amountStyle);
            totalRetailValue = totalRetailValue.add(BigDecimal.valueOf(tRetail));

            Cell expiryCell = row.createCell(10);
            if (p.getExpiryDate() != null) {
                expiryCell.setCellValue(p.getExpiryDate());
                expiryCell.setCellStyle(dateStyle);
            }

            Cell statusCell = row.createCell(11);
            if (stock <= 0) {
                statusCell.setCellValue("Out of Stock");
                statusCell.setCellStyle(dangerStyle);
            } else if (stock <= minStock) {
                statusCell.setCellValue("Low Stock");
                statusCell.setCellStyle(warningStyle);
            } else if (p.getExpiryDate() != null && !p.getExpiryDate().isAfter(LocalDate.now().plusDays(30))) {
                statusCell.setCellValue("Expiring Soon");
                statusCell.setCellStyle(dangerStyle);
            } else {
                statusCell.setCellValue("OK");
            }
        }

        Row summaryRow = sheet.createRow(rowIdx + 1);
        Cell summaryLabelCell = summaryRow.createCell(7);
        summaryLabelCell.setCellValue("Totals:");
        summaryLabelCell.setCellStyle(headerStyle);

        Cell sumCostCell = summaryRow.createCell(8);
        sumCostCell.setCellValue(totalCostValue.doubleValue());
        sumCostCell.setCellStyle(amountStyle);

        Cell sumRetailCell = summaryRow.createCell(9);
        sumRetailCell.setCellValue(totalRetailValue.doubleValue());
        sumRetailCell.setCellStyle(amountStyle);

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createSalesSheet(Workbook workbook, String sheetName, List<Object[]> sales, 
                                  CellStyle headerStyle, CellStyle amountStyle) {
        Sheet sheet = workbook.createSheet(sheetName);

        Row headerRow = sheet.createRow(0);
        String[] columns = {
                "Product Name", "Category", "Current Stock Qty", "Unit", "Total Qty Sold"
        };

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (Object[] ps : sales) {
            Product p = (Product) ps[0];
            double qtySold = ps[1] != null ? ((BigDecimal) ps[1]).doubleValue() : 0.0;

            Row row = sheet.createRow(rowIdx++);

            row.createCell(0).setCellValue(p.getNameEn() != null ? p.getNameEn() : "");
            row.createCell(1).setCellValue(p.getCategory() != null ? p.getCategory() : "");
            
            Cell stockCell = row.createCell(2);
            stockCell.setCellValue(p.getStockQty() != null ? p.getStockQty().doubleValue() : 0.0);
            
            row.createCell(3).setCellValue(p.getUnit() != null ? p.getUnit() : "piece");

            Cell soldCell = row.createCell(4);
            soldCell.setCellValue(qtySold);
        }

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

    private CellStyle createWarningStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(IndexedColors.DARK_YELLOW.getIndex());
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createDangerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
