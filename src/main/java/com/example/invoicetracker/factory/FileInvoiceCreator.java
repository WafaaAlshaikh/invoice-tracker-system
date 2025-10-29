package com.example.invoicetracker.factory;

import com.example.invoicetracker.config.FileValidator;
import com.example.invoicetracker.dto.InvoiceRequest;
import com.example.invoicetracker.model.entity.Invoice;
import com.example.invoicetracker.model.entity.User;
import com.example.invoicetracker.model.enums.FileType;
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
public class FileInvoiceCreator implements InvoiceCreator {

    private final FileValidator fileValidator;
    private final FileProcessor fileProcessor;

    @Override
    public Invoice createInvoice(InvoiceRequest request, User user) throws Exception {
        MultipartFile file = request.getFile();
        
        fileValidator.validateFile(file);
        
        FileProcessingResult processingResult = fileProcessor.process(file);
        
        if (!processingResult.isSuccess()) {
            throw new RuntimeException("File processing failed: " + processingResult.getErrorMessage());
        }

        FileType fileType = processingResult.getFileType();

        String fileName = request.getFileName() != null ? 
                request.getFileName() : processingResult.getOriginalFileName();
        if (fileName == null) {
            fileName = "invoice_" + System.currentTimeMillis();
        }

        log.info("File processed successfully - Type: {}, Size: {}", 
                processingResult.getFileType(), processingResult.getFileSize());

        return Invoice.builder()
                .uploadedByUser(user)
                .invoiceDate(request.getInvoiceDate())
                .fileType(fileType)
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
        return request.getFile() != null && !request.getFile().isEmpty();
    }
}