package com.example.invoicetracker.factory;

import com.example.invoicetracker.dto.InvoiceRequest;
import com.example.invoicetracker.model.entity.Invoice;
import com.example.invoicetracker.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceFactory {

    private final HybridInvoiceCreator hybridInvoiceCreator;
    private final FileInvoiceCreator fileInvoiceCreator;
    private final FormInvoiceCreator formInvoiceCreator;

    public Invoice createInvoice(InvoiceRequest request, User user) throws Exception {
        log.info("=== Invoice Factory: Determining invoice type ===");
        
        boolean hasFile = request.getFile() != null && !request.getFile().isEmpty();
        boolean hasProducts = request.getProductQuantities() != null && !request.getProductQuantities().isEmpty();
        
        log.info("Input Analysis - File: {}, Products: {}", hasFile, hasProducts);

        if (hasFile && hasProducts) {
            log.info("Creating HYBRID invoice (File + Products)");
            return hybridInvoiceCreator.createInvoice(request, user);
        }
        
        if (hasFile) {
            log.info("Creating FILE-BASED invoice (PDF/Image)");
            return fileInvoiceCreator.createInvoice(request, user);
        }
        
        if (hasProducts) {
            log.info("Creating WEB_FORM invoice (Products only)");
            return formInvoiceCreator.createInvoice(request, user);
        }

        throw new IllegalArgumentException(
            "Cannot create invoice: No file or product information provided. " +
            "Please provide either a file, product details, or both.");
    }
}