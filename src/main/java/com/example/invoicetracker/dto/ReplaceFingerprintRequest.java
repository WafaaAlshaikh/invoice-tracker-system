package com.example.invoicetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
class ReplaceFingerprintRequest {
    private Long temporaryId;
    private Long finalInvoiceId;
    private LocalDate invoiceDate;
    private BigDecimal totalAmount;
    private String vendor;
    private String textContent;
    private String username;
    private String role;
}
