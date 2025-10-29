package com.example.invoicetracker.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String description;
    private Boolean isActive;
}
