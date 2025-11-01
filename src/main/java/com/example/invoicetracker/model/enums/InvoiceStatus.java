package com.example.invoicetracker.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Invoice status values")
public enum InvoiceStatus {
    @Schema(description = "Pending approval") PENDING,
    @Schema(description = "Approved") APPROVED,
    @Schema(description = "Rejected") REJECTED,
    @Schema(description = "Completed") COMPLETED
}