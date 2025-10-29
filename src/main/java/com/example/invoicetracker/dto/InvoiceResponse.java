package com.example.invoicetracker.dto;

import com.example.invoicetracker.model.enums.FileType;
import com.example.invoicetracker.model.enums.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class InvoiceResponse {
    private Long invoiceId;
    private String uploadedByUser;
    private LocalDate invoiceDate;
    private FileType fileType;
    private String fileName;
    private Double totalAmount;
    private InvoiceStatus status;

    private List<String> productNames; 
    private List<String> categories;   
}
