package com.example.invoicetracker.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.example.invoicetracker.model.enums.FileType;
import com.example.invoicetracker.model.enums.InvoiceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class InvoiceRequest {
    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;
    
    @NotBlank(message = "File name is required")
    @Size(max = 255, message = "File name must be less than 255 characters")
    private String fileName;
    
    private FileType fileType;
    
    private List<Long> productIds;
    
    private Map<Long, Double> productQuantities;
    
    private String userId;
    
    private MultipartFile file;
    
    private InvoiceStatus status;
}
