package com.example.invoicetracker.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileProcessor {

    private final List<FileProcessingStrategy> strategies;

    public FileProcessingResult process(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return FileProcessingResult.builder()
                    .success(false)
                    .errorMessage("File is empty or null")
                    .build();
        }

        String contentType = file.getContentType();
        log.info("Looking for strategy for file type: {}", contentType);

        for (FileProcessingStrategy strategy : strategies) {
            if (strategy.supports(contentType)) {
                log.info("Using strategy: {}", strategy.getClass().getSimpleName());
                return strategy.process(file);
            }
        }

        log.warn("No specific strategy found for file type: {}", contentType);
        return FileProcessingResult.builder()
                .success(false)
                .errorMessage("Unsupported file type: " + contentType)
                .build();
    }
}