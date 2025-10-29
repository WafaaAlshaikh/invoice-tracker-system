package com.example.invoicetracker.dto;

import lombok.Data;

@Data
public class CategoryFilterRequest {
    private int page = 0;
    private int size = 10;
    private String sortBy = "categoryName";
    private String direction = "ASC";
    private String search;
}
