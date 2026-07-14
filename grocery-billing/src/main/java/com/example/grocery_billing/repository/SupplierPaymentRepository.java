package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.SupplierPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, Long> {
    List<SupplierPayment> findBySupplierIdOrderByPaymentDateDescCreatedAtDesc(Long supplierId);
}
