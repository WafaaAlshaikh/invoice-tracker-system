package com.example.invoicetracker.service;

import com.example.invoicetracker.model.enums.FileType;
import com.example.invoicetracker.model.enums.InvoiceStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataProcessingService {

    private final ObjectMapper objectMapper;

    public List<Long> parseProductIds(String productIds) {
        if (productIds == null || productIds.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            if (productIds.startsWith("[")) {
                return objectMapper.readValue(productIds, new TypeReference<List<Long>>() {});
            } else {
                return Arrays.stream(productIds.split(","))
                        .map(String::trim)
                        .map(Long::valueOf)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid productIds format: " + productIds);
        }
    }

    public Map<Long, Double> parseProductQuantities(String productQuantities) {
        if (productQuantities == null || productQuantities.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            if (productQuantities.startsWith("{")) {
                return objectMapper.readValue(productQuantities, new TypeReference<Map<Long, Double>>() {});
            } else {
                Map<Long, Double> result = new HashMap<>();
                String[] pairs = productQuantities.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        result.put(Long.valueOf(keyValue[0].trim()), Double.valueOf(keyValue[1].trim()));
                    }
                }
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid productQuantities format: " + productQuantities);
        }
    }

    public FileType convertToFileType(String fileType) {
        if (fileType == null) return null;
        try {
            return FileType.valueOf(fileType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public InvoiceStatus convertToInvoiceStatus(String status) {
        if (status == null) return null;
        try {
            return InvoiceStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}