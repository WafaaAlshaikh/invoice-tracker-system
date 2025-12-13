package com.example.invoicetracker.service.external;

import com.example.invoicetracker.dto.DuplicateCheckResponse;
import com.example.invoicetracker.model.entity.InvoiceProduct;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DuplicateCheckClient {
    
    @Value("${duplicate.check.service.url:http://duplicate-check-service:8081}")
    private String duplicateCheckServiceUrl;
    
    private final RestTemplate restTemplate;

    
    public void saveFingerprint(Long invoiceId, LocalDate invoiceDate,
                           BigDecimal totalAmount, String vendor,
                           String textContent, String username, String role) {
    
    log.info("🎯 SAVING FINGERPRINT - Invoice ID: {}", invoiceId);
    
    try {
        Map<String, Object> request = new HashMap<>();
        request.put("invoiceId", invoiceId);
        request.put("invoiceDate", invoiceDate.toString());
        request.put("totalAmount", totalAmount.toString());
        request.put("vendor", vendor);
        request.put("textContent", textContent);
        request.put("username", username);
        request.put("role", role);
        
        String url = duplicateCheckServiceUrl + "/api/duplicate-check/save-fingerprint";
        log.info("🌐 Calling URL: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);
        
        log.info("📤 Request Body: {}", request);
        
        ResponseEntity<Void> response = restTemplate.postForEntity(
            url, 
            requestEntity, 
            Void.class
        );
        
        if (response.getStatusCode().is2xxSuccessful()) {
            log.info("✅ Fingerprint saved successfully for invoice: {}", invoiceId);
        } else {
            log.error("❌ Failed to save fingerprint. Status: {}", response.getStatusCode());
        }
        
    } catch (Exception e) {
        log.error("❌ Error saving fingerprint for invoice {}: {}", invoiceId, e.getMessage());
        log.error("Stack trace:", e);
    }
}
    @Data
    private static class DuplicateCheckRequest {
        private Long invoiceId;
        private LocalDate invoiceDate;
        private BigDecimal totalAmount;
        private String vendor;
        private String textContent;
        private String username;
        private String role;
    }
    
    private DuplicateCheckResponse createDefaultResponse(String error) {
        DuplicateCheckResponse defaultResponse = new DuplicateCheckResponse();
        defaultResponse.setDuplicate(false);
        defaultResponse.setConfidenceScore(BigDecimal.ZERO);
        defaultResponse.setCheckMethod("SERVICE_UNAVAILABLE");
        defaultResponse.setRecommendation("Duplicate check service unavailable. Proceeding with upload.");
        
        return defaultResponse;
    }



    public void saveTemporaryFingerprint(Long temporaryId, LocalDate invoiceDate,
                                   BigDecimal totalAmount, String vendor,
                                   String textContent, String username, String role) {
    
    log.info("💾 SAVING TEMPORARY FINGERPRINT - Temp ID: {}", temporaryId);
    
    try {
        Map<String, Object> request = new HashMap<>();
        request.put("invoiceId", temporaryId);
        request.put("invoiceDate", invoiceDate.toString());
        request.put("totalAmount", totalAmount.toString());
        request.put("vendor", vendor);
        request.put("textContent", textContent);
        request.put("username", username);
        request.put("role", role);
        request.put("isTemporary", true);
        
        String url = duplicateCheckServiceUrl + "/api/duplicate-check/save-temporary";
        log.info("🌐 Calling URL: {}", url);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);
        
        log.info("📤 Request Body: {}", request);
        
        ResponseEntity<Void> response = restTemplate.postForEntity(
            url, 
            requestEntity, 
            Void.class
        );
        
        if (response.getStatusCode().is2xxSuccessful()) {
            log.info("✅ Temporary fingerprint saved: {}", temporaryId);
        } else {
            log.warn("⚠️ Failed to save temporary fingerprint. Status: {}", response.getStatusCode());
        }
        
    } catch (Exception e) {
        log.warn("⚠️ Could not save temporary fingerprint for {}: {}", temporaryId, e.getMessage());
    }
}


public void replaceTemporaryWithFinal(Long temporaryId, Long finalInvoiceId,
                                    LocalDate invoiceDate, BigDecimal totalAmount,
                                    String vendor, List<InvoiceProduct> products,
                                    String username, String role) {
    
    log.info("🔄 REPLACING TEMPORARY WITH FINAL - Temp: {}, Final: {}", 
             temporaryId, finalInvoiceId);
    
    try {
        StringBuilder textContentBuilder = new StringBuilder();
        
        textContentBuilder.append(String.format(
            "InvoiceID:%d|Vendor:%s|Date:%s|Amount:%.2f",
            finalInvoiceId,
            vendor != null ? vendor : "Unknown",
            invoiceDate,
            totalAmount
        ));
        
        if (products != null && !products.isEmpty()) {
            textContentBuilder.append("|Products:");
            for (InvoiceProduct ip : products) {
                textContentBuilder.append(String.format("%s(%.1f),",
                    ip.getProduct().getProductName(),
                    ip.getQuantity()
                ));
            }
        }
        
        textContentBuilder.append("|User:").append(username);
        
        String textContent = textContentBuilder.toString();
        
        Map<String, Object> request = new HashMap<>();
        request.put("temporaryId", temporaryId);
        request.put("finalInvoiceId", finalInvoiceId);
        request.put("invoiceDate", invoiceDate.toString());
        request.put("totalAmount", totalAmount.toString());
        request.put("vendor", vendor != null ? vendor : "Unknown");
        request.put("textContent", textContent);
        request.put("username", username);
        request.put("role", role);
        
        String url = duplicateCheckServiceUrl + "/api/duplicate-check/replace-temporary";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);
        
        ResponseEntity<Void> response = restTemplate.postForEntity(
            url,
            requestEntity,
            Void.class
        );
        
        if (response.getStatusCode().is2xxSuccessful()) {
            log.info("✅ Successfully replaced temporary {} with final {}", 
                     temporaryId, finalInvoiceId);
        } else {
            log.error("❌ Failed to replace temporary fingerprint. Status: {}", 
                     response.getStatusCode());
        }
        
    } catch (Exception e) {
        log.error("❌ Error replacing fingerprint: {}", e.getMessage());
        log.error("Stack trace:", e);
    }
}


public DuplicateCheckResponse checkForDuplicates(MultipartFile file,
                                                LocalDate invoiceDate,
                                                BigDecimal totalAmount,
                                                String vendor,
                                                String username,
                                                String role,
                                                Long invoiceId,  
                                                String fileName) {
    
    log.info("🎯 DUPLICATE CHECK CLIENT - Invoice/Temp ID: {}", invoiceId);
    log.info("📞 Target URL: {}", duplicateCheckServiceUrl);
    
    try {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        
        if (file != null && !file.isEmpty()) {
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", fileResource);
        }
        
        body.add("invoiceDate", invoiceDate.toString());
        body.add("totalAmount", totalAmount != null ? totalAmount.toString() : "0");
        if (vendor != null) {
            body.add("vendor", vendor);
        }
        body.add("username", username);
        body.add("role", role);
        body.add("invoiceId", invoiceId != null ? invoiceId.toString() : "0");
        if (fileName != null) {
            body.add("fileName", fileName);
        }
        
        boolean isTemporary = invoiceId != null && invoiceId > 100000000L; 
        body.add("isTemporary", String.valueOf(isTemporary));
        
        String url = duplicateCheckServiceUrl + "/api/duplicate-check/check";
        log.info("🌐 Calling URL: {}", url);
        
        HttpEntity<MultiValueMap<String, Object>> requestEntity = 
            new HttpEntity<>(body, headers);
        
        ResponseEntity<DuplicateCheckResponse> responseEntity = restTemplate.exchange(
            url,
            HttpMethod.POST,
            requestEntity,
            DuplicateCheckResponse.class
        );
        
        return responseEntity.getBody();
        
    } catch (Exception e) {
        log.error("Error in duplicate check: {}", e.getMessage());
        return createDefaultResponse("Service error: " + e.getMessage());
    }
}
}