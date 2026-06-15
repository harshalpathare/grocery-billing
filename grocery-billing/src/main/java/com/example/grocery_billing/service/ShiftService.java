package com.example.grocery_billing.service;

import com.example.grocery_billing.entity.Shift;
import com.example.grocery_billing.entity.User;
import com.example.grocery_billing.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftService {

    private final ShiftRepository shiftRepository;

    public boolean isShiftActive() {
        return shiftRepository.findByActiveTrue().isPresent();
    }

    public Shift getActiveShift() {
        return shiftRepository.findByActiveTrue().orElse(null);
    }

    @Transactional
    public Shift startShift(User user, BigDecimal openingBalance) {
        if (isShiftActive()) {
            throw new RuntimeException("A shift is already active!");
        }

        Shift shift = Shift.builder()
                .user(user)
                .openingBalance(openingBalance != null ? openingBalance : BigDecimal.ZERO)
                .active(true)
                .startTime(LocalDateTime.now())
                .cashSales(BigDecimal.ZERO)
                .build();

        return shiftRepository.save(shift);
    }

    @Transactional
    public Shift endShift(BigDecimal closingBalance) {
        Shift shift = getActiveShift();
        if (shift == null) {
            throw new RuntimeException("No active shift found to end.");
        }

        shift.setClosingBalance(closingBalance);
        
        // Expected = Opening + Cash Sales
        BigDecimal opening = shift.getOpeningBalance() != null ? shift.getOpeningBalance() : BigDecimal.ZERO;
        BigDecimal sales = shift.getCashSales() != null ? shift.getCashSales() : BigDecimal.ZERO;
        shift.setExpectedBalance(opening.add(sales));
        
        shift.setEndTime(LocalDateTime.now());
        shift.setActive(false);

        return shiftRepository.save(shift);
    }

    @Transactional
    public void addCashSale(BigDecimal amount) {
        Shift shift = getActiveShift();
        if (shift != null && amount != null) {
            BigDecimal currentSales = shift.getCashSales() != null ? shift.getCashSales() : BigDecimal.ZERO;
            shift.setCashSales(currentSales.add(amount));
            shiftRepository.save(shift);
        }
    }

    public List<Shift> getAllShifts() {
        return shiftRepository.findAllByOrderByStartTimeDesc();
    }
}
