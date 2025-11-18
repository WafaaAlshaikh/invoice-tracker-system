package com.example.invoicetracker.factory;

import com.example.invoicetracker.config.FileValidator;
import com.example.invoicetracker.dto.InvoiceExtractionResult;
import com.example.invoicetracker.dto.InvoiceRequest;
import com.example.invoicetracker.model.entity.Invoice;
import com.example.invoicetracker.model.entity.User;
import com.example.invoicetracker.model.enums.InvoiceStatus;
import com.example.invoicetracker.service.ai.InvoiceExtractorService;
import com.example.invoicetracker.strategy.FileProcessingResult;
import com.example.invoicetracker.strategy.FileProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@Component
@RequiredArgsConstructor
public class FileInvoiceCreator implements InvoiceCreator {

    private final FileValidator fileValidator;
    private final FileProcessor fileProcessor;
    private final InvoiceExtractorService invoiceExtractorService; 


    @Override
    public Invoice createInvoice(InvoiceRequest request, User user) throws Exception {
        MultipartFile file = request.getFile();
        
        log.info("Processing file with AI extraction: {}", file.getOriginalFilename());
        
        fileValidator.validateFile(file);
        
        FileProcessingResult processingResult = fileProcessor.process(file);
        
        if (!processingResult.isSuccess()) {
            throw new RuntimeException("File processing failed: " + processingResult.getErrorMessage());
        }

        //  AI Extraction
        Double extractedAmount = null;
        try {
            InvoiceExtractionResult extractionResult = invoiceExtractorService.extractInvoiceData(file);
            if (extractionResult.isSuccess()) {
                extractedAmount = extractionResult.getTotalAmount();
                log.info("AI extracted amount: ${}", extractedAmount);
            }
        } catch (Exception e) {
            log.warn("AI extraction failed, using default: {}", e.getMessage());
        }

        String fileName = determineFileName(request.getFileName(), processingResult.getOriginalFileName());

        Invoice invoice = Invoice.builder()
                .uploadedByUser(user)
                .invoiceDate(request.getInvoiceDate())
                .fileType(processingResult.getFileType())
                .fileName(fileName)
                .storedFileName(processingResult.getStoredFileName())
                .originalFileName(processingResult.getOriginalFileName())
                .fileSize(processingResult.getFileSize())
                .status(InvoiceStatus.PENDING)
                .isActive(true)
                .totalAmount(extractedAmount != null ? extractedAmount : 0.0) 
                .build();

        return invoice;
    }

    @Override
    public boolean supports(InvoiceRequest request) {
        boolean hasFile = request.getFile() != null && !request.getFile().isEmpty();
        boolean noProducts = request.getProductQuantities() == null || request.getProductQuantities().isEmpty();
        return hasFile && noProducts;
    }

    private String determineFileName(String providedName, String originalName) {
        if (providedName != null && !providedName.trim().isEmpty()) {
            return providedName;
        }
        if (originalName != null && !originalName.trim().isEmpty()) {
            return originalName;
        }
        return "invoice_" + System.currentTimeMillis();
    }
}

