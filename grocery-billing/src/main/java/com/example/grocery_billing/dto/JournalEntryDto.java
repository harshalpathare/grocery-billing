package com.example.grocery_billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntryDto {
    private LocalDateTime date;
    private String reference; // e.g. "BILL-001", "EXP-12", "TRX-45"
    private String account; // The account that this leg of the transaction touches
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal runningBalance; // Computed in backend for ledger views
    private String narration; // e.g. "Cash Sales", "Payment to Supplier XYZ"
    
    // For sorting easily
    public LocalDateTime getDate() {
        return date;
    }
}
