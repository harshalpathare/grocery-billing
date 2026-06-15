package com.example.grocery_billing.repository;

import com.example.grocery_billing.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    
    Optional<Shift> findByActiveTrue();
    
    List<Shift> findAllByOrderByStartTimeDesc();
}
