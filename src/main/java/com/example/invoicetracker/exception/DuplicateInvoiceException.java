package com.example.invoicetracker.exception;

import lombok.Getter;
import java.math.BigDecimal;
import java.util.List;

@Getter
public class DuplicateInvoiceException extends RuntimeException {
    
    private final BigDecimal confidenceScore;
    private final List<?> similarInvoices;
    
    public DuplicateInvoiceException(String message, 
                                   BigDecimal confidenceScore, 
                                   List<?> similarInvoices) {
        super(message);
        this.confidenceScore = confidenceScore;
        this.similarInvoices = similarInvoices;
    }
    
    public DuplicateInvoiceException(String message) {
        super(message);
        this.confidenceScore = BigDecimal.ZERO;
        this.similarInvoices = null;
    }
}