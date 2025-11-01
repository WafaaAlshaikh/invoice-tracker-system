package com.example.invoicetracker.controller;

import com.example.invoicetracker.controller.util.AuthHelper;
import com.example.invoicetracker.dto.*;
import com.example.invoicetracker.service.InvoiceService;
import com.example.invoicetracker.service.InvoiceUploadRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Slf4j
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "APIs for managing invoices")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceUploadRequestMapper requestMapper;
    private final AuthHelper authHelper;

    private Pageable createPageable(int page, int size, String sortBy, String direction) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
    }

    // ==================== CREATE / UPLOAD ====================
    

     // Create invoice with JSON (for Web Form)
     
    @PreAuthorize("hasRole('USER') or hasRole('SUPERUSER')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Create invoice (JSON)", 
        description = "Creates invoice with product data only (Web Form)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice created successfully", 
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<InvoiceResponse> createInvoiceJson(
            @Validated @RequestBody InvoiceRequest request,
            @Parameter(hidden = true) Authentication authentication) {

        Map<String, String> userInfo = authHelper.getUserInfo(authentication);
        log.info("Creating invoice (JSON) for user: {}, role: {}", 
                userInfo.get("username"), userInfo.get("role"));

        InvoiceResponse response = invoiceService.createInvoice(
                request, 
                userInfo.get("username"), 
                userInfo.get("role"));
        
        return ResponseEntity.ok(response);
    }

     // Create/Upload invoice with file (multipart)
     
    @PreAuthorize("hasRole('USER') or hasRole('SUPERUSER')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload invoice with file", 
        description = "Creates invoice with file and optional product data"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice uploaded successfully", 
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<InvoiceResponse> uploadInvoice(
            @Validated @ModelAttribute InvoiceUploadRequest uploadRequest,
            @Parameter(hidden = true) Authentication authentication) {

        Map<String, String> userInfo = authHelper.getUserInfo(authentication);
        log.info("Uploading invoice for user: {}, role: {}", 
                userInfo.get("username"), userInfo.get("role"));

        InvoiceRequest request = requestMapper.prepareInvoiceUploadRequest(uploadRequest);
        
        InvoiceResponse response = invoiceService.createInvoice(
                request, 
                userInfo.get("username"), 
                userInfo.get("role"));
        
        return ResponseEntity.ok(response);
    }

    // ==================== UPDATE ====================
    
    
     //Update invoice with JSON

    @PreAuthorize("hasRole('USER') or hasRole('SUPERUSER')")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Update invoice (JSON)", 
        description = "Updates invoice with product data"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice updated successfully", 
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Invoice not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<InvoiceResponse> updateInvoiceJson(
            @PathVariable Long id,
            @Validated @RequestBody InvoiceRequest request,
            Authentication authentication) {

        Map<String, String> userInfo = authHelper.getUserInfo(authentication);
        log.info("Updating invoice {} (JSON) by user: {}, role: {}", 
                id, userInfo.get("username"), userInfo.get("role"));

        InvoiceResponse response = invoiceService.updateInvoice(
                id, 
                request, 
                userInfo.get("username"), 
                userInfo.get("role"));
        
        return ResponseEntity.ok(response);
    }

    
     // Update invoice with file (multipart)
     
    @PreAuthorize("hasRole('USER') or hasRole('SUPERUSER')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Update invoice with file", 
        description = "Updates invoice with file and/or product data"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice updated successfully", 
                    content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Invoice not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<InvoiceResponse> updateInvoiceMultipart(
            @PathVariable Long id,
            @Validated @ModelAttribute InvoiceUploadRequest uploadRequest,
            Authentication authentication) {

        Map<String, String> userInfo = authHelper.getUserInfo(authentication);
        log.info("Updating invoice {} (multipart) by user: {}, role: {}", 
                id, userInfo.get("username"), userInfo.get("role"));

        InvoiceRequest request = requestMapper.prepareInvoiceUpdateRequest(
                uploadRequest.getFile(),
                uploadRequest.getInvoiceDate(),
                uploadRequest.getProductIds(),
                uploadRequest.getProductQuantities(),
                uploadRequest.getFileName());

        InvoiceResponse response = invoiceService.updateInvoice(
                id, 
                request, 
                userInfo.get("username"), 
                userInfo.get("role"));
        
        return ResponseEntity.ok(response);
    }

    // ==================== DELETE ====================
    @PreAuthorize("hasRole('USER') or hasRole('SUPERUSER')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete invoice", description = "Soft deletes an invoice")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id, Authentication authentication) {
        Map<String, String> userInfo = authHelper.getUserInfo(authentication);
        invoiceService.deleteInvoice(id, userInfo.get("username"), userInfo.get("role"));
        return ResponseEntity.noContent().build();
    }

    // ==================== DOWNLOAD ====================
    @PreAuthorize("hasAnyRole('USER','SUPERUSER','AUDITOR')")
    @GetMapping("/{id}/download")
    @Operation(summary = "Download invoice file", description = "Downloads the invoice file (PDF/Image)")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id, Authentication authentication) {
        Map<String, String> userInfo = authHelper.getUserInfo(authentication);
        return invoiceService.downloadInvoiceFile(id, userInfo.get("username"), userInfo.get("role"));
    }

    // ==================== GET ====================
    @PreAuthorize("hasAnyRole('USER','SUPERUSER','AUDITOR')")
    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID", description = "Retrieves a specific invoice with optional details")
    public ResponseEntity<?> getInvoice(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean includeDetails) {

        Map<String, String> userInfo = authHelper.getUserInfo(authentication);
        if (includeDetails) {
            return ResponseEntity.ok(
                    invoiceService.getInvoiceDetails(id, userInfo.get("username"), userInfo.get("role")));
        } else {
            InvoiceResponse response = invoiceService.getInvoiceById(
                    id, userInfo.get("username"), userInfo.get("role"));
            return ResponseEntity.ok(response);
        }
    }

    // ==================== LIST ====================
    @PreAuthorize("hasRole('SUPERUSER') or hasRole('AUDITOR')")
    @GetMapping("/all")
    @Operation(summary = "Get all invoices", description = "Retrieves all invoices (SUPERUSER/AUDITOR only)")
    public ResponseEntity<Page<InvoiceResponse>> getAllInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        Pageable pageable = createPageable(page, size, sortBy, direction);
        return ResponseEntity.ok(invoiceService.getAllInvoices(pageable));
    }

    @PreAuthorize("hasAnyRole('USER','SUPERUSER','AUDITOR')")
    @GetMapping
    @Operation(summary = "Get user invoices", description = "Retrieves invoices for the current user")
    public ResponseEntity<Page<InvoiceResponse>> getUserInvoices(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        Map<String, String> userInfo = authHelper.getUserInfo(authentication);
        Pageable pageable = createPageable(page, size, sortBy, direction);
        return ResponseEntity.ok(invoiceService.getUserInvoices(userInfo.get("username"), pageable));
    }

    // ==================== SEARCH ====================
    @PreAuthorize("hasAnyRole('USER','SUPERUSER','AUDITOR')")
    @GetMapping("/search")
    @Operation(summary = "Search invoices", description = "Search invoices with various criteria")
    public ResponseEntity<Page<InvoiceResponse>> searchInvoices(
            @ModelAttribute InvoiceSearchRequest searchRequest,
            Authentication authentication) {

        Map<String, String> userInfo = authHelper.getUserInfo(authentication);
        Page<InvoiceResponse> result = invoiceService.searchInvoices(
                searchRequest, 
                userInfo.get("username"),
                userInfo.get("role"));
        return ResponseEntity.ok(result);
    }

    // ==================== STATS ====================
    @PreAuthorize("hasAnyRole('USER','SUPERUSER','AUDITOR')")
    @GetMapping("/stats")
    @Operation(summary = "Get invoice statistics", description = "Retrieves statistics about invoices")
    public ResponseEntity<Map<String, Object>> getInvoiceStats(Authentication authentication) {
        Map<String, String> userInfo = authHelper.getUserInfo(authentication);
        Map<String, Object> stats = invoiceService.getInvoiceStats(
                userInfo.get("username"), 
                userInfo.get("role"));
        return ResponseEntity.ok(stats);
    }

    // ==================== AUDIT LOGS ====================
    @PreAuthorize("hasAnyRole('USER','SUPERUSER','AUDITOR')")
    @GetMapping("/{id}/audit-logs")
    @Operation(summary = "Get invoice audit logs", description = "Retrieves audit logs for a specific invoice")
    public ResponseEntity<List<AuditLogResponse>> getInvoiceAuditLogs(
            @PathVariable Long id,
            Authentication authentication) {

        Map<String, String> userInfo = authHelper.getUserInfo(authentication);
        return ResponseEntity.ok(
                invoiceService.getInvoiceAuditLogs(id, userInfo.get("username"), userInfo.get("role")));
    }
}