package com.example.invoicetracker.strategy;

import com.example.invoicetracker.model.enums.FileType;
import com.example.invoicetracker.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfProcessingStrategy implements FileProcessingStrategy {

    private final FileStorageService fileStorageService;

    @Override
    public FileProcessingResult process(MultipartFile file) {
        try {
            log.info("Processing PDF file: {}", file.getOriginalFilename());
            
            String storedFileName = fileStorageService.storeFile(file);
            
            return FileProcessingResult.builder()
                    .storedFileName(storedFileName)
                    .originalFileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .fileType(FileType.PDF)
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error processing PDF file: {}", e.getMessage());
            return FileProcessingResult.builder()
                    .success(false)
                    .errorMessage("Failed to process PDF: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean supports(String fileType) {
        return "application/pdf".equalsIgnoreCase(fileType);
    }
}