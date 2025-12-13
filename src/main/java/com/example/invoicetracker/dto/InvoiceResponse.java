package com.example.invoicetracker.dto;

import com.example.invoicetracker.model.enums.FileType;
import com.example.invoicetracker.model.enums.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@Schema(description = "Invoice response")
public class InvoiceResponse {
    @Schema(description = "Invoice ID", example = "1")
    private Long invoiceId;
    
    @Schema(description = "Username who uploaded the invoice", example = "john_doe")
    private String uploadedByUser;
    
    @Schema(description = "Invoice date", example = "2024-01-15")
    private LocalDate invoiceDate;
    
    @Schema(description = "File type")
    private FileType fileType;
    
    @Schema(description = "File name", example = "january_invoice.pdf")
    private String fileName;
    
    @Schema(description = "Total amount", example = "1500.75")
    private Double totalAmount;
    
    @Schema(description = "Invoice status")
    private InvoiceStatus status;

    @Schema(description = "List of product names", example = "[\"Laptop\", \"Mouse\"]")
    private List<String> productNames; 
    
    @Schema(description = "List of categories", example = "[\"Electronics\", \"Accessories\"]")
    private List<String> categories;   
}