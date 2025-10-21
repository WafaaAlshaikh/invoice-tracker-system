package com.example.invoicetracker.model.entity;

import com.example.invoicetracker.model.entity.base.BaseEntity;
import com.example.invoicetracker.model.enums.FileType;
import com.example.invoicetracker.model.enums.InvoiceStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "invoice")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "uploaded_by", length = 50, nullable = false)
    @NotBlank(message = "Uploaded by is required")
    private String uploadedBy;

    @Column(name = "invoice_date", nullable = false)
    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    @NotNull(message = "File type is required")
    private FileType fileType;

    @Column(name = "file_name", nullable = false)
    @NotBlank(message = "File name is required")
    private String fileName;

    @Column(name = "total_amount", nullable = false)
    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status = InvoiceStatus.PENDING;


    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceProduct> invoiceProduct;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Log> log;

   
}
