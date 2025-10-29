package com.example.invoicetracker.factory;

import com.example.invoicetracker.dto.InvoiceRequest;
import com.example.invoicetracker.model.entity.Invoice;
import com.example.invoicetracker.model.entity.User;

public interface InvoiceCreator {
    Invoice createInvoice(InvoiceRequest request, User user) throws Exception;
    boolean supports(InvoiceRequest request);
}