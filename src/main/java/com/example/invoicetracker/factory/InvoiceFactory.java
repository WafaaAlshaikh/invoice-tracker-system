package com.example.invoicetracker.factory;

import com.example.invoicetracker.dto.InvoiceRequest;
import com.example.invoicetracker.model.entity.Invoice;
import com.example.invoicetracker.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceFactory {

    private final List<InvoiceCreator> creators;

    public Invoice createInvoice(InvoiceRequest request, User user) throws Exception {
        log.info("Creating invoice using factory - File: {}, Products: {}", 
                request.getFile() != null, 
                request.getProductQuantities() != null);
        
        for (InvoiceCreator creator : creators) {
            if (creator.supports(request)) {
                log.info("Using creator: {}", creator.getClass().getSimpleName());
                return creator.createInvoice(request, user);
            }
        }
        
        throw new IllegalArgumentException("No suitable invoice creator found for the provided request");
    }
}