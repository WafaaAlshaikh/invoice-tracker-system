package com.example.invoicetracker.service;

import com.example.invoicetracker.dto.InvoiceRequest;
import com.example.invoicetracker.dto.InvoiceUploadRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InvoiceUploadRequestMapper {

    private final DataProcessingService dataProcessingService;

    public InvoiceRequest prepareInvoiceUploadRequest(InvoiceUploadRequest uploadRequest) {
        return prepareInvoiceUploadRequest(
                uploadRequest.getFile(),
                uploadRequest.getInvoiceDate(),
                uploadRequest.getProductIds(),
                uploadRequest.getProductQuantities(),
                uploadRequest.getUserId(),
                uploadRequest.getFileName()
        );
    }

    private InvoiceRequest prepareInvoiceUploadRequest(
            MultipartFile file,
            String invoiceDate,
            String productIds,
            String productQuantities,
            String userId,
            String fileName) {
        
        InvoiceRequest request = new InvoiceRequest();
        request.setFile(file);
        
        if (invoiceDate != null && !invoiceDate.trim().isEmpty()) {
            request.setInvoiceDate(LocalDate.parse(invoiceDate));
        }
        
        if (productIds != null || productQuantities != null) {
            List<Long> productIdsList = dataProcessingService.parseProductIds(productIds);
            Map<Long, Double> productQuantitiesMap = dataProcessingService.parseProductQuantities(productQuantities);
            
            request.setProductIds(productIdsList);
            request.setProductQuantities(productQuantitiesMap);
        } else {
            request.setProductIds(new ArrayList<>());
            request.setProductQuantities(new HashMap<>());
        }
        
        request.setUserId(userId);
        request.setFileName(fileName);

        return request;
    }

    public InvoiceRequest prepareInvoiceUpdateRequest(
            MultipartFile file,
            String invoiceDate,
            String productIds,
            String productQuantities,
            String fileName) {
        
        return prepareInvoiceUploadRequest(file, invoiceDate, productIds, productQuantities, null, fileName);
    }
}