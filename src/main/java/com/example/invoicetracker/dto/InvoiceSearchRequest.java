package com.example.invoicetracker.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class InvoiceSearchRequest {
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdAt";
    private String direction = "DESC";
    private String search;
    private LocalDate startDate;
    private LocalDate endDate;
    private String fileType;
    private String status;
}