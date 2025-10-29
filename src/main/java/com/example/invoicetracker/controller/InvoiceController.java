package com.example.invoicetracker.controller;

import com.example.invoicetracker.dto.*;
import com.example.invoicetracker.service.InvoiceService;
import com.example.invoicetracker.service.InvoiceUploadRequestMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceUploadRequestMapper requestMapper;

    @PreAuthorize("hasRole('USER') or hasRole('SUPERUSER')")
    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Validated @RequestBody InvoiceRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String role = extractRole(authentication);
        log.info("Creating invoice for user: {}, role: {}", username, role);
        return ResponseEntity.ok(invoiceService.createInvoice(request, username, role));
    }

    @PreAuthorize("hasRole('USER') or hasRole('SUPERUSER')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InvoiceResponse> uploadInvoice(
            @ModelAttribute InvoiceUploadRequest uploadRequest,
            Authentication authentication) {

        String username = authentication.getName();
        String role = extractRole(authentication);

        InvoiceRequest request = requestMapper.prepareInvoiceUploadRequest(uploadRequest);
        return ResponseEntity.ok(invoiceService.createInvoice(request, username, role));
    }

    @PreAuthorize("hasAnyRole('USER','SUPERUSER','AUDITOR')")
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        String role = extractRole(authentication);
        return invoiceService.downloadInvoiceFile(id, username, role);
    }

    @PreAuthorize("hasRole('SUPERUSER') or hasRole('AUDITOR')")
    @GetMapping("/all")
    public ResponseEntity<Page<InvoiceResponse>> getAllInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        return ResponseEntity.ok(invoiceService.getAllInvoices(pageable));
    }

    @PreAuthorize("hasAnyRole('USER','SUPERUSER','AUDITOR')")
    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> getUserInvoices(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        String username = authentication.getName();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(direction), sortBy));
        return ResponseEntity.ok(invoiceService.getUserInvoices(username, pageable));
    }

    @PreAuthorize("hasAnyRole('USER','SUPERUSER','AUDITOR')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getInvoice(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean includeDetails) {

        String username = authentication.getName();
        String role = extractRole(authentication);

        if (includeDetails) {
            return ResponseEntity.ok(invoiceService.getInvoiceDetails(id, username, role));
        } else {
            InvoiceResponse response = invoiceService.getInvoiceById(id, username, role);
            return ResponseEntity.ok(response);
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('SUPERUSER')")
    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @PathVariable Long id,
            @Validated @RequestBody InvoiceRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String role = extractRole(authentication);
        log.info("Updating invoice {} by user: {}, role: {}", id, username, role);
        return ResponseEntity.ok(invoiceService.updateInvoice(id, request, username, role));
    }

    @PreAuthorize("hasRole('USER') or hasRole('SUPERUSER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        String role = extractRole(authentication);
        invoiceService.deleteInvoice(id, username, role);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('USER','SUPERUSER','AUDITOR')")
    @GetMapping("/search")
    public ResponseEntity<Page<InvoiceResponse>> searchInvoices(
            @ModelAttribute InvoiceSearchRequest searchRequest,
            Authentication authentication) {
        String username = authentication.getName();
        String role = extractRole(authentication);
        Page<InvoiceResponse> result = invoiceService.searchInvoices(searchRequest, username, role);
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAnyRole('USER','SUPERUSER','AUDITOR')")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getInvoiceStats(Authentication authentication) {
        String username = authentication.getName();
        String role = extractRole(authentication);
        Map<String, Object> stats = invoiceService.getInvoiceStats(username, role);
        return ResponseEntity.ok(stats);
    }

    @PreAuthorize("hasAnyRole('USER','SUPERUSER','AUDITOR')")
    @GetMapping("/{id}/audit-logs")
    public ResponseEntity<List<AuditLogResponse>> getInvoiceAuditLogs(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        String role = extractRole(authentication);
        return ResponseEntity.ok(invoiceService.getInvoiceAuditLogs(id, username, role));
    }

    @PreAuthorize("hasRole('USER') or hasRole('SUPERUSER')")
    @PutMapping(value = "/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InvoiceResponse> updateInvoiceWithFile(
            @PathVariable Long id,
            @Validated @ModelAttribute InvoiceUploadRequest uploadRequest,
            Authentication authentication) {

        String username = authentication.getName();
        String role = extractRole(authentication);

        InvoiceRequest request = requestMapper.prepareInvoiceUpdateRequest(
                uploadRequest.getFile(),
                uploadRequest.getInvoiceDate(),
                uploadRequest.getProductIds(),
                uploadRequest.getProductQuantities(),
                uploadRequest.getFileName());

        return ResponseEntity.ok(invoiceService.updateInvoice(id, request, username, role));
    }

    @PreAuthorize("hasRole('USER') or hasRole('SUPERUSER')")
    @PutMapping("/{id}/full")
    public ResponseEntity<InvoiceResponse> updateInvoiceFull(
            @PathVariable Long id,
            @Validated @RequestBody InvoiceRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String role = extractRole(authentication);
        return ResponseEntity.ok(invoiceService.updateInvoiceFull(id, request, username, role));
    }

    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
}