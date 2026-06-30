package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.*;
import com.example.grocery_billing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderService {

        private final PurchaseOrderRepository     poRepository;
        private final ProductRepository           productRepository;
        private final SupplierRepository          supplierRepository;

    // ── READ ─────────────────────────────────────────────
    public List<PurchaseOrder> getAll() {
        return poRepository.findAll(
                Sort.by(Sort.Direction.DESC,
                        "orderDate", "id"));
    }

    public PurchaseOrder getById(Long id) {
        return poRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "PO not found: " + id));
    }

    public List<PurchaseOrder> getBySupplier(Long supplierId) {
        return poRepository.findBySupplierId(supplierId,
                Sort.by(Sort.Direction.DESC, "orderDate"));
    }

    // ── GENERATE PO NUMBER ────────────────────────────────
    public String generatePoNumber() {
        String year   = String.valueOf(
                LocalDate.now().getYear());
        int    serial = 1;

        Optional<String> last =
                poRepository.findLastPoForYear(year);
        if (last.isPresent() && !last.get().isBlank()) {
            try {
                String[] parts = last.get().split("-");
                serial = Integer.parseInt(
                        parts[parts.length - 1]) + 1;
            } catch (Exception e) { serial = 1; }
        }

        String candidate;
        do {
            candidate = String.format(
                    "PO-%s-%04d", year, serial++);
        } while (poRepository.existsByPoNumber(candidate));

        return candidate;
    }

    // ── CREATE PURCHASE ORDER ─────────────────────────────
    /**
     * Creates a PO and:
     * 1. Saves all items with cost price
     * 2. Updates product stock quantity
     * 3. Updates product cost price
     * 4. Updates supplier payable balance
     */
    @Transactional
    public void delete(Long id) {
        PurchaseOrder po = getById(id);

        // ✅ Reverse stock changes before deleting
        for (PurchaseOrderItem item : po.getItems()) {
            Product product = item.getProduct();
            if (product != null && item.getQuantity() != null) {
                BigDecimal current = product.getStockQty() != null
                        ? product.getStockQty() : BigDecimal.ZERO;
                BigDecimal updated = current.subtract(item.getQuantity());
                product.setStockQty(updated.max(BigDecimal.ZERO));
                productRepository.save(product);
            }
        }

        // ✅ Reverse supplier balance
        if (po.getSupplier() != null) {
            Supplier supplier = supplierRepository
                    .findById(po.getSupplier().getId())
                    .orElse(null);
            if (supplier != null
                    && po.getTotalAmount() != null) {
                BigDecimal current =
                        supplier.getTotalPayable() != null
                                ? supplier.getTotalPayable()
                                : BigDecimal.ZERO;
                supplier.setTotalPayable(
                        current.subtract(po.getTotalAmount())
                                .max(BigDecimal.ZERO));
                supplier.updateBalance();
                supplierRepository.save(supplier);
            }
        }

        poRepository.delete(po);
    }
    @Transactional
    public void markAsPaid(Long id, BigDecimal amountPaid) {
        PurchaseOrder po = getById(id);

        po.setAmountPaid(amountPaid != null
                ? amountPaid : po.getTotalAmount());

        BigDecimal total = po.getTotalAmount() != null
                ? po.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal paid  = po.getAmountPaid() != null
                ? po.getAmountPaid() : BigDecimal.ZERO;

        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            po.setPaymentStatus(
                    PurchaseOrder.PaymentStatus.PENDING);
        } else if (paid.compareTo(total) >= 0) {
            po.setPaymentStatus(
                    PurchaseOrder.PaymentStatus.PAID);
        } else {
            po.setPaymentStatus(
                    PurchaseOrder.PaymentStatus.PARTIAL);
        }

        poRepository.save(po);

        // ✅ UPDATE SUPPLIER HERE — only from PO pay
        if (po.getSupplier() != null) {
            Supplier supplier = supplierRepository
                    .findById(po.getSupplier().getId())
                    .orElse(null);
            if (supplier != null) {
                BigDecimal currentPaid =
                        supplier.getTotalPaid() != null
                                ? supplier.getTotalPaid()
                                : BigDecimal.ZERO;
                supplier.setTotalPaid(
                        currentPaid.add(paid));
                supplier.updateBalance();
                supplierRepository.save(supplier);
            }
        }
    }
    @Transactional
    public PurchaseOrder createPurchaseOrder(
            PurchaseOrder po,
            List<PoItemRequest> itemRequests) {

        if (po.getPoNumber() == null
                || po.getPoNumber().isBlank()) {
            po.setPoNumber(generatePoNumber());
        }
        if (po.getOrderDate() == null) {
            po.setOrderDate(LocalDate.now());
        }

        // Process items
        for (PoItemRequest req : itemRequests) {
            Product product = productRepository
                    .findById(req.productId())
                    .orElseThrow(() -> new RuntimeException(
                            "Product not found"));

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setProduct(product);
            item.setProductNameSnapshot(product.getNameEn());
            item.setQuantity(req.quantity());
            item.setUnitCost(req.unitCost());
            item.calculateTotal();
            po.addItem(item);

            // ✅ Update product stock
            BigDecimal currentStock = product.getStockQty() != null
                    ? product.getStockQty() : BigDecimal.ZERO;
            BigDecimal addQty = req.quantity();
            product.setStockQty(currentStock.add(addQty));

            // ✅ Update product cost price
            // Uses the latest purchase price
            product.setCostPrice(req.unitCost());
            productRepository.save(product);

            log.info("Stock updated: {} +{} = {}",
                    product.getNameEn(), addQty.stripTrailingZeros().toPlainString(),
                    product.getStockQty().stripTrailingZeros().toPlainString());
        }

        // Calculate totals
        po.calculateTotals();

        // Save PO
        PurchaseOrder saved = poRepository.save(po);

        // ✅ Update supplier payable
        if (po.getSupplier() != null) {
            Supplier supplier = supplierRepository
                    .findById(po.getSupplier().getId())
                    .orElse(null);
            if (supplier != null) {
                BigDecimal current =
                        supplier.getTotalPayable() != null
                                ? supplier.getTotalPayable()
                                : BigDecimal.ZERO;
                supplier.setTotalPayable(
                        current.add(saved.getTotalAmount()));
                supplier.updateBalance();
                supplierRepository.save(supplier);
            }
        }

        return saved;
    }
    @Transactional
    public PurchaseOrder saveDirectly(PurchaseOrder po) {
        return poRepository.save(po);
    }
    // ── DTO ───────────────────────────────────────────────
    public record PoItemRequest(
            Long       productId,
            BigDecimal quantity,
            BigDecimal unitCost
    ) {}
}