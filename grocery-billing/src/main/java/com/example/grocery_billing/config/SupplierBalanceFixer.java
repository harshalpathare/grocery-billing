package com.example.grocery_billing.config;

import com.example.grocery_billing.entity.PurchaseOrder;
import com.example.grocery_billing.entity.Supplier;
import com.example.grocery_billing.repository.PurchaseOrderRepository;
import com.example.grocery_billing.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * One-time runner to fix corrupted supplier balances
 */
@Component
@RequiredArgsConstructor
public class SupplierBalanceFixer implements CommandLineRunner {

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository poRepository;
    private static final Logger log = LoggerFactory.getLogger(SupplierBalanceFixer.class);

    @Override
    public void run(String... args) throws Exception {
        log.info("Running one-time supplier balance fix...");
        List<Supplier> suppliers = supplierRepository.findAll();
        for (Supplier s : suppliers) {
            BigDecimal totalPayable = BigDecimal.ZERO;
            BigDecimal totalPaid = BigDecimal.ZERO;

            List<PurchaseOrder> pos = poRepository.findBySupplierId(s.getId(), org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "orderDate"));
            for (PurchaseOrder po : pos) {
                if (po.getTotalAmount() != null) {
                    if (po.getType() == PurchaseOrder.OrderType.RETURN) {
                        totalPayable = totalPayable.subtract(po.getTotalAmount());
                    } else {
                        totalPayable = totalPayable.add(po.getTotalAmount());
                    }
                }
                
                if (po.getAmountPaid() != null && po.getType() != PurchaseOrder.OrderType.RETURN) {
                    totalPaid = totalPaid.add(po.getAmountPaid());
                }
            }

            // Ensure not negative payable
            if (totalPayable.compareTo(BigDecimal.ZERO) < 0) {
                totalPayable = BigDecimal.ZERO;
            }

            s.setTotalPayable(totalPayable);
            s.setTotalPaid(totalPaid);
            s.updateBalance(); // Balance = Payable - Paid
            supplierRepository.save(s);
            log.info("Fixed Supplier {}: Payable={}, Paid={}, Balance={}", s.getName(), totalPayable, totalPaid, s.getBalance());
        }
    }
}
