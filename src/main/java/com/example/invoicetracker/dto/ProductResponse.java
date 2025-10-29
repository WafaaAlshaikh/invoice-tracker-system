
package com.example.invoicetracker.dto;

import lombok.*;

import java.math.BigDecimal;

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
    private BigDecimal unitPrice;
    private Boolean isActive;
}
