package com.example.invoicetracker.strategy;

import com.example.invoicetracker.model.enums.FileType;
import com.example.invoicetracker.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageProcessingStrategy implements FileProcessingStrategy {

    private final FileStorageService fileStorageService;
    
    private static final Set<String> SUPPORTED_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/bmp"
    );

    @Override
    public FileProcessingResult process(MultipartFile file) {
        try {
            log.info("Processing image file: {}", file.getOriginalFilename());
            
            String storedFileName = fileStorageService.storeFile(file);
                        
            return FileProcessingResult.builder()
                    .storedFileName(storedFileName)
                    .originalFileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .fileType(FileType.IMAGE)
                    .success(true)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error processing image file: {}", e.getMessage());
            return FileProcessingResult.builder()
                    .success(false)
                    .errorMessage("Failed to process image: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean supports(String fileType) {
        return fileType != null && SUPPORTED_TYPES.contains(fileType.toLowerCase());
    }
}