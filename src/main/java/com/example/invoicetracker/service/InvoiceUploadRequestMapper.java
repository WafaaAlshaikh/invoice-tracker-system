package com.example.invoicetracker.service;

import com.example.invoicetracker.dto.InvoiceRequest;
import com.example.invoicetracker.dto.InvoiceUploadRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class InvoiceUploadRequestMapper {

    private final DataProcessingService dataProcessingService;

    public InvoiceRequest prepareInvoiceUploadRequest(InvoiceUploadRequest uploadRequest) {
        InvoiceRequest request = new InvoiceRequest();
        request.setFile(uploadRequest.getFile());
        
        if (uploadRequest.getInvoiceDate() != null && !uploadRequest.getInvoiceDate().trim().isEmpty()) {
            try {
                request.setInvoiceDate(LocalDate.parse(uploadRequest.getInvoiceDate()));
            } catch (Exception e) {
                throw new RuntimeException("Invalid date format. Use YYYY-MM-DD");
            }
        } else {
            request.setInvoiceDate(LocalDate.now());
        }
        
        processProducts(request, uploadRequest);
        
        request.setUserId(uploadRequest.getUserId());
        request.setFileName(uploadRequest.getFileName());

        log.debug("Mapped request - File: {}, ProductIds: {}, Quantities: {}", 
                request.getFile() != null, 
                request.getProductIds() != null ? request.getProductIds().size() : 0,
                request.getProductQuantities() != null ? request.getProductQuantities().size() : 0);

        return request;
    }

    private void processProducts(InvoiceRequest request, InvoiceUploadRequest uploadRequest) {
        Map<Long, Double> productQuantitiesMap = new HashMap<>();
        
        if (uploadRequest.getProductIds() != null && !uploadRequest.getProductIds().trim().isEmpty()) {
            List<Long> productIds = dataProcessingService.parseProductIds(uploadRequest.getProductIds());
            
            if (uploadRequest.getProductQuantities() != null && !uploadRequest.getProductQuantities().trim().isEmpty()) {
                productQuantitiesMap = dataProcessingService.parseProductQuantities(uploadRequest.getProductQuantities());
            } else {
                for (Long productId : productIds) {
                    productQuantitiesMap.put(productId, 1.0);
                }
            }
        } else if (uploadRequest.getProductQuantities() != null && !uploadRequest.getProductQuantities().trim().isEmpty()) {
            productQuantitiesMap = dataProcessingService.parseProductQuantities(uploadRequest.getProductQuantities());
        }
        
        request.setProductQuantities(productQuantitiesMap);
        
        if (!productQuantitiesMap.isEmpty()) {
            request.setProductIds(new ArrayList<>(productQuantitiesMap.keySet()));
        } else {
            request.setProductIds(new ArrayList<>());
        }
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