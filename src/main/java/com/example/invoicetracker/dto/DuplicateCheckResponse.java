package com.example.invoicetracker.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DuplicateCheckResponse {
    private boolean duplicate;
    private BigDecimal confidenceScore;
    private String checkMethod;
    private List<SimilarInvoice> similarInvoices;
    private String recommendation;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SimilarInvoice {
        private Long invoiceId;
        private String fileName;
        private LocalDate uploadDate;
        private BigDecimal totalAmount;
        private BigDecimal similarityScore;
        private String matchReason;
    }
}