
package com.example.invoicetracker.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long productId;
    private Long categoryId;
    private String categoryName;
    private String productCode;
    private String productName;
    private String description;
    private Double unitPrice;
    private Boolean isActive;
}
