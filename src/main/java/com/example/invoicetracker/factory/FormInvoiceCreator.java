package com.example.invoicetracker.factory;

import com.example.invoicetracker.dto.InvoiceRequest;
import com.example.invoicetracker.model.entity.Invoice;
import com.example.invoicetracker.model.entity.User;
import com.example.invoicetracker.model.enums.FileType;
import com.example.invoicetracker.model.enums.InvoiceStatus;
import org.springframework.stereotype.Component;

@Component
public class FormInvoiceCreator implements InvoiceCreator {

    @Override
    public Invoice createInvoice(InvoiceRequest request, User user) {
        if (request.getProductQuantities() == null || request.getProductQuantities().isEmpty()) {
            throw new IllegalArgumentException("For form-based invoices, product quantities are required");
        }

        return Invoice.builder()
                .uploadedByUser(user)
                .invoiceDate(request.getInvoiceDate())
                .fileType(FileType.WEB_FORM)
                .fileName(request.getFileName())
                .status(InvoiceStatus.PENDING)
                .isActive(true)
                .totalAmount(0.0)
                .build();
    }

    @Override
    public boolean supports(InvoiceRequest request) {
        return (request.getFile() == null || request.getFile().isEmpty()) &&
               request.getProductQuantities() != null && 
               !request.getProductQuantities().isEmpty();
    }
}