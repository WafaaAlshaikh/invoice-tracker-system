package com.example.invoicetracker.factory;

import com.example.invoicetracker.config.FileValidator;
import com.example.invoicetracker.dto.InvoiceRequest;
import com.example.invoicetracker.model.entity.Invoice;
import com.example.invoicetracker.model.entity.User;
import com.example.invoicetracker.model.enums.InvoiceStatus;
import com.example.invoicetracker.strategy.FileProcessingResult;
import com.example.invoicetracker.strategy.FileProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@Component
@RequiredArgsConstructor
public class HybridInvoiceCreator implements InvoiceCreator {

    private final FileValidator fileValidator;
    private final FileProcessor fileProcessor;

    @Override
    public Invoice createInvoice(InvoiceRequest request, User user) throws Exception {
        MultipartFile file = request.getFile();
        
        log.info("Processing hybrid invoice - File: {}, Products: {}", 
                file.getOriginalFilename(), 
                request.getProductQuantities().size());
        
        fileValidator.validateFile(file);
        
        FileProcessingResult processingResult = fileProcessor.process(file);
        
        if (!processingResult.isSuccess()) {
            throw new RuntimeException("File processing failed: " + processingResult.getErrorMessage());
        }

        String fileName = determineFileName(request.getFileName(), processingResult.getOriginalFileName());

        log.info("Hybrid invoice - FileType: {}, Products count: {}", 
                processingResult.getFileType(),
                request.getProductQuantities().size());

        return Invoice.builder()
                .uploadedByUser(user)
                .invoiceDate(request.getInvoiceDate())
                .fileType(processingResult.getFileType()) 
                .fileName(fileName)
                .storedFileName(processingResult.getStoredFileName())
                .originalFileName(processingResult.getOriginalFileName())
                .fileSize(processingResult.getFileSize())
                .status(InvoiceStatus.PENDING)
                .isActive(true)
                .totalAmount(0.0)
                .build();
    }

    @Override
    public boolean supports(InvoiceRequest request) {
        boolean hasFile = request.getFile() != null && !request.getFile().isEmpty();
        boolean hasProducts = request.getProductQuantities() != null && !request.getProductQuantities().isEmpty();
        return hasFile && hasProducts;
    }

    private String determineFileName(String providedName, String originalName) {
        if (providedName != null && !providedName.trim().isEmpty()) {
            return providedName;
        }
        if (originalName != null && !originalName.trim().isEmpty()) {
            return originalName;
        }
        return "hybrid_invoice_" + System.currentTimeMillis();
    }
}