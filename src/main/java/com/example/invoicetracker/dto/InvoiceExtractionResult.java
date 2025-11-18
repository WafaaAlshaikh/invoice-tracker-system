package com.example.invoicetracker.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceExtractionResult {
    private boolean success;
    private String errorMessage;
    private String rawResponse;
    
    private LocalDate invoiceDate;
    private Double totalAmount;
    private String vendor;
    private List<ExtractedItem> extractedItems;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractedItem {
        private String name;
        private Double quantity;
        private Double unitPrice;
        private Double subtotal;
    }
}