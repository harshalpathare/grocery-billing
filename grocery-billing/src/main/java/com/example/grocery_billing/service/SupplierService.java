package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Supplier;
import com.example.grocery_billing.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public List<Supplier> getAllActive() {
        return supplierRepository
                .findByActiveTrueOrderByNameAsc();
    }

    public Supplier getById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Supplier not found: " + id));
    }

    @Transactional
    public Supplier save(Supplier supplier) {
        if (supplier.getName() != null)
            supplier.setName(supplier.getName().trim());
        if (supplier.getPhone() != null)
            supplier.setPhone(supplier.getPhone().trim());
        return supplierRepository.save(supplier);
    }

    @Transactional
    public void delete(Long id) {
        Supplier s = getById(id);
        s.setActive(false);
        supplierRepository.save(s);
    }

    public BigDecimal getTotalPayable() {
        return supplierRepository.getTotalPayable();
    }

    @Transactional
    public void recordPayment(Long supplierId,
                              BigDecimal amount) {
        Supplier s = getById(supplierId);

        BigDecimal currentPaid = s.getTotalPaid() != null
                ? s.getTotalPaid() : BigDecimal.ZERO;
        BigDecimal payable = s.getTotalPayable() != null
                ? s.getTotalPayable() : BigDecimal.ZERO;

        // ✅ Clamp — cannot pay more than balance
        BigDecimal maxPayable = payable.subtract(currentPaid);
        BigDecimal safeAmount = amount.min(maxPayable);

        if (safeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(
                    "No pending balance to pay.");
        }

        s.setTotalPaid(currentPaid.add(safeAmount));
        s.updateBalance();
        supplierRepository.saveAndFlush(s);
    }
}