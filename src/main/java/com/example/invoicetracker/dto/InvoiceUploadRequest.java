package com.example.invoicetracker.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class InvoiceUploadRequest {
    private MultipartFile file;
    private String invoiceDate;
    private String productIds;
    private String productQuantities;
    private String userId;
    private String fileName;
}   