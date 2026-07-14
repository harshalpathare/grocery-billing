package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Product;
import com.example.grocery_billing.entity.Shop;
import com.example.grocery_billing.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;

@Service
@RequiredArgsConstructor
public class ExcelProductService {

    private final ProductRepository productRepository;
    private final com.example.grocery_billing.repository.ShopRepository shopRepository;
    private final Validator validator;
    
    // Headers for the Excel file
    private static final String[] HEADERS = {
            "ID", "Barcode", "SKU", "Name (EN)", "Name (HI)", "Name (MR)",
            "Category", "Brand", "Unit", "Cost Price", "Selling Price",
            "MRP", "GST %", "HSN Code", "Stock Qty", "Min Stock", "Batch Number", "Expiry Date (YYYY-MM-DD)", "Active"
    };

    private Shop currentShop() {
        Long id = com.example.grocery_billing.config.ShopContext.getShopId();
        if (id == null) throw new RuntimeException("No shop in context");
        return shopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shop not found: " + id));
    }

    /**
     * Exports a list of products to an Excel file and writes to the output stream.
     */
    public void exportToExcel(List<Product> products, OutputStream os) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Products");

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
            for (Product product : products) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(product.getId() != null ? product.getId().toString() : "");
                row.createCell(1).setCellValue(product.getBarcode() != null ? product.getBarcode() : "");
                row.createCell(2).setCellValue(product.getSku() != null ? product.getSku() : "");
                row.createCell(3).setCellValue(product.getNameEn() != null ? product.getNameEn() : "");
                row.createCell(4).setCellValue(product.getNameHi() != null ? product.getNameHi() : "");
                row.createCell(5).setCellValue(product.getNameMr() != null ? product.getNameMr() : "");
                row.createCell(6).setCellValue(product.getCategory() != null ? product.getCategory() : "");
                row.createCell(7).setCellValue(product.getBrand() != null ? product.getBrand() : "");
                row.createCell(8).setCellValue(product.getUnit() != null ? product.getUnit() : "");
                
                row.createCell(9).setCellValue(product.getCostPrice() != null ? product.getCostPrice().doubleValue() : 0.0);
                row.createCell(10).setCellValue(product.getPrice() != null ? product.getPrice().doubleValue() : 0.0);
                row.createCell(11).setCellValue(product.getMrp() != null ? product.getMrp().doubleValue() : 0.0);
                row.createCell(12).setCellValue(product.getGstPercent() != null ? product.getGstPercent().doubleValue() : 0.0);
                
                row.createCell(13).setCellValue(product.getHsnCode() != null ? product.getHsnCode() : "");
                row.createCell(14).setCellValue(product.getStockQty() != null ? product.getStockQty().doubleValue() : 0.0);
                row.createCell(15).setCellValue(product.getMinStock() != null ? product.getMinStock().doubleValue() : 0.0);
                row.createCell(16).setCellValue(product.getBatchNumber() != null ? product.getBatchNumber() : "");
                
                row.createCell(17).setCellValue(product.getExpiryDate() != null ? product.getExpiryDate().toString() : "");
                row.createCell(18).setCellValue(product.getActive() != null && product.getActive() ? "Yes" : "No");
            }

            // Auto-size columns
            for (int col = 0; col < HEADERS.length; col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(os);
        }
    }

    /**
     * Imports products from an uploaded Excel file.
     * Matches existing products by Barcode -> SKU -> Name (EN).
     */
    @Transactional
    public int importFromExcel(MultipartFile file) throws IOException {
        Shop currentShop = currentShop();
        int count = 0;
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();
                
                // Skip header
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                // Break if row is completely empty
                if (isRowEmpty(currentRow)) {
                    break;
                }

                Product product = parseRow(currentRow, currentShop);
                if (product != null) {
                    java.util.Set<ConstraintViolation<Product>> violations = validator.validate(product);
                    if (!violations.isEmpty()) {
                        String errMsg = violations.iterator().next().getMessage();
                        throw new RuntimeException("Row " + (rowNumber + 1) + " invalid: " + errMsg);
                    }
                    productRepository.save(product);
                    count++;
                }
                rowNumber++;
            }
        }
        return count;
    }

    private Product parseRow(Row row, Shop shop) {
        String barcode = getCellStringValue(row.getCell(1));
        String sku = getCellStringValue(row.getCell(2));
        String nameEn = getCellStringValue(row.getCell(3));

        // Skip if there's no name and no barcode
        if ((nameEn == null || nameEn.isBlank()) && (barcode == null || barcode.isBlank())) {
            return null;
        }

        // Try to match existing product
        Product product = matchExistingProduct(shop, barcode, sku, nameEn);
        if (product == null) {
            product = new Product();
            product.setShop(shop);
            product.setActive(true);
        }

        // Name (EN) is usually required or one of the language names
        product.setBarcode(barcode);
        product.setSku(sku);
        product.setNameEn(nameEn);
        product.setNameHi(getCellStringValue(row.getCell(4)));
        product.setNameMr(getCellStringValue(row.getCell(5)));
        product.setCategory(getCellStringValue(row.getCell(6)));
        product.setBrand(getCellStringValue(row.getCell(7)));
        
        String unit = getCellStringValue(row.getCell(8));
        product.setUnit(unit != null && !unit.isBlank() ? unit : "piece");

        product.setCostPrice(getCellBigDecimalValue(row.getCell(9), BigDecimal.ZERO));
        product.setPrice(getCellBigDecimalValue(row.getCell(10), BigDecimal.ZERO));
        product.setMrp(getCellBigDecimalValue(row.getCell(11), null));
        product.setGstPercent(getCellBigDecimalValue(row.getCell(12), BigDecimal.ZERO));
        
        product.setHsnCode(getCellStringValue(row.getCell(13)));
        product.setStockQty(getCellBigDecimalValue(row.getCell(14), BigDecimal.ZERO));
        product.setMinStock(getCellBigDecimalValue(row.getCell(15), BigDecimal.ZERO));
        product.setBatchNumber(getCellStringValue(row.getCell(16)));

        String expiryDateStr = getCellStringValue(row.getCell(17));
        if (expiryDateStr != null && !expiryDateStr.isBlank()) {
            try {
                product.setExpiryDate(LocalDate.parse(expiryDateStr, DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (Exception e) {
                // Ignore invalid dates
            }
        }
        
        String activeStr = getCellStringValue(row.getCell(18));
        if (activeStr != null && (activeStr.equalsIgnoreCase("No") || activeStr.equalsIgnoreCase("False"))) {
            product.setActive(false);
        } else {
            product.setActive(true);
        }

        return product;
    }

    private Product matchExistingProduct(Shop shop, String barcode, String sku, String nameEn) {
        Long shopId = shop.getId();
        
        if (barcode != null && !barcode.isBlank()) {
            Product match = productRepository.findByShopIdAndBarcode(shopId, barcode.trim()).orElse(null);
            if (match != null) return match;
        }
        if (sku != null && !sku.isBlank()) {
            Product match = productRepository.findByShopIdAndSku(shopId, sku.trim()).orElse(null);
            if (match != null) return match;
        }
        if (nameEn != null && !nameEn.isBlank()) {
            Product match = productRepository.findByShopIdAndNameEn(shopId, nameEn.trim()).orElse(null);
            if (match != null) return match;
        }
        
        return null;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        DataFormatter formatter = new DataFormatter();
        String val = formatter.formatCellValue(cell).trim();
        return val.isEmpty() ? null : val;
    }

    private BigDecimal getCellBigDecimalValue(Cell cell, BigDecimal defaultValue) {
        if (cell == null) return defaultValue;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim().replaceAll(",", "");
                if (val.isEmpty()) return defaultValue;
                return new BigDecimal(val);
            }
        } catch (Exception e) {
            // ignore
        }
        return defaultValue;
    }
}
