package com.example.invoicetracker.factory;

import com.example.invoicetracker.dto.InvoiceRequest;
import com.example.invoicetracker.model.entity.Invoice;
import com.example.invoicetracker.model.entity.User;
import com.example.invoicetracker.model.enums.FileType;
import com.example.invoicetracker.model.enums.InvoiceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class FormInvoiceCreator implements InvoiceCreator {

    @Override
    public Invoice createInvoice(InvoiceRequest request, User user) {
        if (request.getProductQuantities() == null || request.getProductQuantities().isEmpty()) {
            throw new IllegalArgumentException("Product information is required for web form invoices");
        }

        String fileName = determineFileName(request.getFileName(), user.getUsername());

        log.info("Creating WEB_FORM invoice with {} products", request.getProductQuantities().size());

        return Invoice.builder()
                .uploadedByUser(user)
                .invoiceDate(request.getInvoiceDate())
                .fileType(FileType.WEB_FORM) 
                .fileName(fileName)
                .storedFileName(null) 
                .originalFileName(null)
                .fileSize(null)
                .status(InvoiceStatus.PENDING)
                .isActive(true)
                .totalAmount(0.0)
                .build();
    }

    @Override
    public boolean supports(InvoiceRequest request) {
        boolean noFile = request.getFile() == null || request.getFile().isEmpty();
        boolean hasProducts = request.getProductQuantities() != null && !request.getProductQuantities().isEmpty();
        return noFile && hasProducts;
    }

    private String determineFileName(String providedName, String username) {
        if (providedName != null && !providedName.trim().isEmpty()) {
            return providedName;
        }
        return "webform_" + username + "_" + System.currentTimeMillis();
    }
}

