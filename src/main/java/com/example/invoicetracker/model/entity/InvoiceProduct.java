package com.example.invoicetracker.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_product")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be positive")
    @Column(name = "quantity", nullable = false)
    private Double quantity; 

    @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be positive")
    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @DecimalMin(value = "0.0", inclusive = false, message = "Subtotal must be positive")
    @Column(name = "subtotal", nullable = false)
    private BigDecimal subtotal;
}
