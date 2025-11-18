package com.example.invoicetracker.service;

import com.example.invoicetracker.dto.*;
import com.example.invoicetracker.exception.ResourceNotFoundException;
import com.example.invoicetracker.factory.InvoiceFactory;
import com.example.invoicetracker.model.entity.*;
import com.example.invoicetracker.model.enums.ActionType;
import com.example.invoicetracker.model.enums.FileType;
import com.example.invoicetracker.model.enums.InvoiceStatus;
import com.example.invoicetracker.repository.InvoiceRepository;
import com.example.invoicetracker.repository.ProductRepository;
import com.example.invoicetracker.repository.UserRepository;
import com.example.invoicetracker.service.ai.InvoiceExtractorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class InvoiceService {
    private final InvoiceExtractorService invoiceExtractorService;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InvoiceFactory invoiceFactory;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;

    // ==================== CREATE INVOICE ====================
    public InvoiceResponse createInvoice(InvoiceRequest request, String username, String role) {
        boolean hasFile = request.getFile() != null && !request.getFile().isEmpty();
        boolean hasProducts = request.getProductQuantities() != null && !request.getProductQuantities().isEmpty();

        log.info("=== Creating Invoice ===");
        log.info("User: {}, Role: {}", username, role);
        log.info("Request Data - HasFile: {}, HasProducts: {}, ProductCount: {}",
                hasFile,
                hasProducts,
                hasProducts ? request.getProductQuantities().size() : 0);

        if (hasProducts) {
            log.info("Product IDs: {}", request.getProductQuantities().keySet());
        }

        User currentUser = getUserByUsername(username);
        validateInvoiceCreation(role);
        User invoiceOwner = determineInvoiceOwner(username, role, request.getUserId());

        validateInvoiceRequest(request);

        Invoice invoice;
        try {
            invoice = invoiceFactory.createInvoice(request, invoiceOwner);
        } catch (Exception e) {
            log.error("Failed to create invoice: {}", e.getMessage());
            throw new RuntimeException("Failed to create invoice: " + e.getMessage());
        }

        // ==================== AI EXTRACTION LOGIC ====================
        Double extractedTotalAmount = extractTotalAmountFromFile(request);
        if (extractedTotalAmount != null) {
            log.info("✅ AI extracted total amount: ${}", extractedTotalAmount);
            invoice.setTotalAmount(extractedTotalAmount);
        } else {
            // Fallback to traditional calculation
            double totalAmount = processInvoiceProducts(invoice, request);
            invoice.setTotalAmount(totalAmount);
            log.info("📊 Using traditional calculation - Amount: ${}", totalAmount);
        }

        auditLogService.logInvoiceAction(invoice, currentUser, ActionType.CREATE, null, null);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        log.info("🎉 Invoice created successfully - ID: {}, Type: {}, Amount: ${}",
                savedInvoice.getInvoiceId(),
                savedInvoice.getFileType(),
                savedInvoice.getTotalAmount());

        return toInvoiceResponse(savedInvoice);
    }

    /**
     * Extract total amount from file using AI if available
     */
    private Double extractTotalAmountFromFile(InvoiceRequest request) {
        try {
            if (request.getFile() != null && !request.getFile().isEmpty()) {
                String contentType = request.getFile().getContentType();

                if (contentType != null &&
                        (contentType.startsWith("image/") || contentType.equals("application/pdf"))) {

                    log.info("🔍 Attempting AI extraction for file: {} (Type: {})",
                            request.getFile().getOriginalFilename(), contentType);

                    InvoiceExtractionResult extractionResult = invoiceExtractorService
                            .extractInvoiceData(request.getFile());

                    if (extractionResult.isSuccess() && extractionResult.getTotalAmount() != null) {
                        log.info("✅ AI extraction successful - Total Amount: ${}", extractionResult.getTotalAmount());

                        if (extractionResult.getInvoiceDate() != null) {
                            log.info("📅 Extracted invoice date: {}", extractionResult.getInvoiceDate());
                        }
                        if (extractionResult.getVendor() != null) {
                            log.info("🏢 Extracted vendor: {}", extractionResult.getVendor());
                        }

                        return extractionResult.getTotalAmount();
                    } else {
                        log.warn("❌ AI extraction failed or no amount found: {}",
                                extractionResult.getErrorMessage());
                    }
                } else {
                    log.info("⏭️ Skipping AI extraction - Unsupported file type: {}", contentType);
                }
            } else {
                log.info("⏭️ No file available for AI extraction");
            }
        } catch (Exception e) {
            log.error("❌ Error during AI extraction: {}", e.getMessage());

        }
        return null;
    }

    // ==================== UPDATE INVOICE ====================
    @Transactional
    public InvoiceResponse updateInvoice(Long id, InvoiceRequest request, String username, String role) {
        log.info("Updating invoice - ID: {}, User: {}, Role: {}", id, username, role);

        Invoice invoice = getActiveInvoice(id);
        User currentUser = getUserByUsername(username);
        validateInvoiceModification(invoice, username, role);

        Map<String, Object> oldValues = auditLogService.captureInvoiceState(invoice);

        if (request.getInvoiceDate() != null) {
            invoice.setInvoiceDate(request.getInvoiceDate());
        }

        if (request.getFileName() != null && !request.getFileName().trim().isEmpty()) {
            invoice.setFileName(request.getFileName());
        }

        if ("SUPERUSER".equals(role) && request.getStatus() != null) {
            invoice.setStatus(request.getStatus());
        }

        // Calculate total amount with AI extraction support
        double totalAmount = calculateTotalAmountForUpdate(invoice, request);
        invoice.setTotalAmount(totalAmount);

        Map<String, Object> newValues = auditLogService.captureInvoiceState(invoice);
        auditLogService.logInvoiceAction(invoice, currentUser, ActionType.UPDATE, oldValues, newValues);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice updated successfully - ID: {}, New Amount: ${}", id, totalAmount);

        return toInvoiceResponse(saved);
    }

    // ==================== DELETE INVOICE ====================
    public void deleteInvoice(Long id, String username, String role) {
        Invoice invoice = getActiveInvoice(id);
        validateInvoiceModification(invoice, username, role);

        invoice.setIsActive(false);
        User currentUser = getUserByUsername(username);
        auditLogService.logInvoiceAction(invoice, currentUser, ActionType.DELETE, null, null);

        invoiceRepository.save(invoice);
        log.info("Invoice deleted (soft delete) - ID: {}", id);
    }

    // ==================== GET INVOICE ====================
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(Long id, String username, String role) {
        Invoice invoice = getActiveInvoice(id);
        validateInvoiceAccess(invoice, username, role);
        return toInvoiceResponse(invoice);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAllInvoices(Pageable pageable) {
        return invoiceRepository.findAllByIsActiveTrue(pageable)
                .map(this::toInvoiceResponse);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getUserInvoices(String username, Pageable pageable) {
        User user = getUserByUsername(username);
        return invoiceRepository.findAllByUploadedByUserAndIsActiveTrue(user, pageable)
                .map(this::toInvoiceResponse);
    }

    // ==================== SEARCH METHODS ====================
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> searchInvoices(InvoiceSearchRequest searchRequest, String username, String role) {
        Pageable pageable = createPageable(searchRequest);

        if (role.equals("USER")) {
            User user = getUserByUsername(username);
            return searchUserInvoices(searchRequest, user, pageable);
        } else {
            return searchAllInvoices(searchRequest, pageable);
        }
    }

    private Page<InvoiceResponse> searchUserInvoices(InvoiceSearchRequest searchRequest, User user, Pageable pageable) {
        if (!hasSearchCriteria(searchRequest)) {
            return invoiceRepository.findAllByUploadedByUserAndIsActiveTrue(user, pageable)
                    .map(this::toInvoiceResponse);
        }

        FileType fileType = convertToFileType(searchRequest.getFileType());
        InvoiceStatus status = convertToInvoiceStatus(searchRequest.getStatus());

        Page<Invoice> invoicePage = invoiceRepository.searchUserInvoices(
                user, searchRequest.getSearch(), searchRequest.getStartDate(),
                searchRequest.getEndDate(), fileType, status, pageable);

        return invoicePage.map(this::toInvoiceResponse);
    }

    private Page<InvoiceResponse> searchAllInvoices(InvoiceSearchRequest searchRequest, Pageable pageable) {
        if (!hasSearchCriteria(searchRequest)) {
            return invoiceRepository.findAllByIsActiveTrue(pageable)
                    .map(this::toInvoiceResponse);
        }

        FileType fileType = convertToFileType(searchRequest.getFileType());
        InvoiceStatus status = convertToInvoiceStatus(searchRequest.getStatus());

        Page<Invoice> invoicePage = invoiceRepository.searchAllInvoices(
                searchRequest.getSearch(), searchRequest.getStartDate(),
                searchRequest.getEndDate(), fileType, status, pageable);

        return invoicePage.map(this::toInvoiceResponse);
    }

    // ==================== FILE DOWNLOAD ====================
    public ResponseEntity<byte[]> downloadInvoiceFile(Long id, String username, String role) {
        Invoice invoice = getActiveInvoice(id);
        validateInvoiceAccess(invoice, username, role);

        if (invoice.getFileType() == FileType.WEB_FORM ||
                invoice.getStoredFileName() == null ||
                invoice.getStoredFileName().isEmpty()) {
            throw new ResourceNotFoundException("This invoice has no file to download (WEB_FORM type)");
        }

        try {
            byte[] fileContent = fileStorageService.loadFile(invoice.getStoredFileName());
            String contentType = fileStorageService.determineContentType(invoice.getFileType());

            return fileStorageService.createFileResponse(fileContent, contentType, invoice.getOriginalFileName());
        } catch (Exception e) {
            throw new ResourceNotFoundException("File not found: " + e.getMessage());
        }
    }

    // ==================== DETAILS & STATS ====================
    @Transactional(readOnly = true)
    public InvoiceDetailsResponse getInvoiceDetails(Long id, String username, String role) {
        Invoice invoice = getActiveInvoice(id);
        validateInvoiceAccess(invoice, username, role);
        return toInvoiceDetailsResponse(invoice);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getInvoiceAuditLogs(Long id, String username, String role) {
        Invoice invoice = getActiveInvoice(id);
        validateInvoiceAccess(invoice, username, role);

        return invoice.getLog().stream()
                .map(this::toAuditLogResponse)
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .collect(Collectors.toList());
    }

    @Transactional
    public InvoiceResponse updateInvoiceStatus(Long id, String newStatus, String username, String role) {
        log.info("Updating invoice status - ID: {}, New Status: {}, User: {}, Role: {}",
                id, newStatus, username, role);

        Invoice invoice = getActiveInvoice(id);

        if (!role.equals("SUPERUSER")) {
            throw new AccessDeniedException("Only SUPERUSER can update invoice status");
        }

        User currentUser = getUserByUsername(username);
        InvoiceStatus oldStatus = invoice.getStatus();
        InvoiceStatus status = InvoiceStatus.valueOf(newStatus.toUpperCase());

        auditLogService.logStatusChange(invoice, currentUser, oldStatus, status);
        invoice.setStatus(status);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice status updated successfully - ID: {}, From: {} To: {}",
                id, oldStatus, status);

        return toInvoiceResponse(saved);
    }

    public Map<String, Object> getInvoiceStats(String username, String role) {
        User user = getUserByUsername(username);
        Map<String, Object> stats = new HashMap<>();

        if (role.equals("USER")) {
            long totalInvoices = invoiceRepository.countByUploadedByUserAndIsActiveTrue(user);
            Double totalAmount = invoiceRepository.sumTotalAmountByUserAndIsActiveTrue(user);

            stats.put("totalInvoices", totalInvoices);
            stats.put("totalAmount", totalAmount != null ? totalAmount : 0.0);
            stats.put("averageAmount",
                    totalInvoices > 0 ? (totalAmount != null ? totalAmount : 0.0) / totalInvoices : 0);
        } else {
            long totalInvoices = invoiceRepository.countByIsActiveTrue();
            Double totalAmount = invoiceRepository.sumTotalAmountByIsActiveTrue();
            long totalUsers = userRepository.countByIsActiveTrue();

            stats.put("totalInvoices", totalInvoices);
            stats.put("totalAmount", totalAmount != null ? totalAmount : 0.0);
            stats.put("totalUsers", totalUsers);
            stats.put("averageAmount",
                    totalInvoices > 0 ? (totalAmount != null ? totalAmount : 0.0) / totalInvoices : 0);
        }

        return stats;
    }

    // ==================== VALIDATION METHODS ====================
    private void validateInvoiceRequest(InvoiceRequest request) {
        boolean hasFile = request.getFile() != null && !request.getFile().isEmpty();
        boolean hasProducts = request.getProductQuantities() != null && !request.getProductQuantities().isEmpty();

        if (!hasFile && !hasProducts) {
            throw new IllegalArgumentException(
                    "Invoice must have either a file or product information. Please provide at least one.");
        }

        log.debug("Invoice request validation passed - HasFile: {}, HasProducts: {}", hasFile, hasProducts);
    }

    private void validateInvoiceAccess(Invoice invoice, String username, String role) {
        if (!role.equals("SUPERUSER") && !role.equals("AUDITOR") &&
                !invoice.getUploadedByUser().getUsername().equals(username)) {
            throw new AccessDeniedException("You cannot access this invoice");
        }
    }

    private void validateInvoiceModification(Invoice invoice, String username, String role) {
        if (!role.toUpperCase().contains("SUPERUSER") &&
                !invoice.getUploadedByUser().getUsername().equals(username)) {
            throw new AccessDeniedException("You cannot modify this invoice");
        }
    }

    private void validateInvoiceCreation(String role) {
        if (!(role.toUpperCase().contains("USER") || role.toUpperCase().contains("SUPERUSER"))) {
            throw new AccessDeniedException("You are not allowed to create an invoice");
        }
    }

    // ==================== HELPER METHODS ====================
    
    /**
     * Calculate total amount for invoice update.
     * Priority: AI extraction > Products calculation > Keep existing total
     */
    private double calculateTotalAmountForUpdate(Invoice invoice, InvoiceRequest request) {
        // Priority 1: Try AI extraction if new file is provided
        if (request.getFile() != null && !request.getFile().isEmpty()) {
            log.info("🔍 New file provided during update - Attempting AI extraction");
            Double extractedAmount = extractTotalAmountFromFile(request);
            
            if (extractedAmount != null && extractedAmount > 0) {
                log.info("✅ AI extraction successful during update - Amount: ${}", extractedAmount);
                return extractedAmount;
            } else {
                log.warn("⚠️ AI extraction failed for new file during update");
            }
        }
        
        // Priority 2: Calculate from products if provided
        if (request.getProductQuantities() != null && !request.getProductQuantities().isEmpty()) {
            log.info("📦 Products provided - Calculating total from products");
            double productTotal = processInvoiceProducts(invoice, request);
            log.info("✅ Calculated total from products: ${}", productTotal);
            return productTotal;
        }
        
        // Priority 3: Keep existing total if no new data provided
        if (invoice.getTotalAmount() != null && invoice.getTotalAmount() > 0) {
            log.info("💾 No new file or products - Keeping existing total: ${}", invoice.getTotalAmount());
            return invoice.getTotalAmount();
        }
        
        // Fallback: Return 0 if nothing else is available
        log.warn("⚠️ No total amount source available - Returning 0");
        return 0.0;
    }
    
    private double processInvoiceProducts(Invoice invoice, InvoiceRequest request) {
        if (request.getProductQuantities() == null || request.getProductQuantities().isEmpty()) {
            if (invoice.getInvoiceProduct() != null) {
                invoice.getInvoiceProduct().clear();
            }
            log.debug("No products to process for invoice");
            return 0.0;
        }

        List<Product> products = productRepository.findAllByIdAndIsActiveTrue(request.getProductQuantities().keySet());
        if (products.isEmpty()) {
            throw new ResourceNotFoundException("No valid products found with provided IDs");
        }

        Set<Long> requestedIds = request.getProductQuantities().keySet();
        Set<Long> foundIds = products.stream()
                .map(Product::getProductId)
                .collect(Collectors.toSet());

        Set<Long> missingIds = requestedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toSet());

        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException("Some products are not found or inactive: " + missingIds);
        }

        List<InvoiceProduct> invoiceProducts = products.stream()
                .map(product -> {
                    double quantity = request.getProductQuantities().getOrDefault(product.getProductId(), 0.0);
                    BigDecimal unitPrice = BigDecimal.valueOf(product.getUnitPrice());
                    BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

                    return InvoiceProduct.builder()
                            .invoice(invoice)
                            .product(product)
                            .quantity(quantity)
                            .unitPrice(unitPrice)
                            .subtotal(subtotal)
                            .build();
                })
                .collect(Collectors.toList());

        if (invoice.getInvoiceProduct() == null) {
            invoice.setInvoiceProduct(new ArrayList<>());
        } else {
            invoice.getInvoiceProduct().clear();
        }
        invoice.getInvoiceProduct().addAll(invoiceProducts);

        double totalAmount = invoiceProducts.stream()
                .mapToDouble(ip -> ip.getSubtotal().doubleValue())
                .sum();

        log.debug("Processed {} products, Total Amount: ${}", invoiceProducts.size(), totalAmount);
        return totalAmount;
    }

    private User determineInvoiceOwner(String username, String role, String targetUserId) {
        User currentUser = getUserByUsername(username);

        if (role.equals("SUPERUSER") && targetUserId != null && !targetUserId.trim().isEmpty()) {
            log.info("SUPERUSER creating invoice for another user: {}", targetUserId);

            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Target user not found: " + targetUserId));

            if (!targetUser.getIsActive()) {
                throw new AccessDeniedException("Cannot create invoice for inactive user: " + targetUserId);
            }

            log.info("Invoice will be owned by: {} (userId: {})", targetUser.getUsername(), targetUserId);
            return targetUser;
        }

        log.info("Invoice will be owned by current user: {}", currentUser.getUsername());
        return currentUser;
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private Invoice getActiveInvoice(Long id) {
        return invoiceRepository.findById(id)
                .filter(Invoice::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    }

    private Pageable createPageable(InvoiceSearchRequest searchRequest) {
        Sort.Direction direction = Sort.Direction.fromString(searchRequest.getDirection());
        Sort sort = Sort.by(direction, searchRequest.getSortBy());
        return PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
    }

    private boolean hasSearchCriteria(InvoiceSearchRequest searchRequest) {
        return (searchRequest.getSearch() != null && !searchRequest.getSearch().trim().isEmpty()) ||
                searchRequest.getStartDate() != null ||
                searchRequest.getEndDate() != null ||
                searchRequest.getFileType() != null ||
                searchRequest.getStatus() != null;
    }

    private FileType convertToFileType(String fileType) {
        if (fileType == null)
            return null;
        try {
            return FileType.valueOf(fileType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private InvoiceStatus convertToInvoiceStatus(String status) {
        if (status == null)
            return null;
        try {
            return InvoiceStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ==================== MAPPING METHODS ====================
    private InvoiceResponse toInvoiceResponse(Invoice invoice) {
        List<String> productNames = invoice.getInvoiceProduct() == null ? List.of()
                : invoice.getInvoiceProduct().stream()
                        .map(ip -> ip.getProduct().getProductName())
                        .collect(Collectors.toList());

        List<String> categories = invoice.getInvoiceProduct() == null ? List.of()
                : invoice.getInvoiceProduct().stream()
                        .map(ip -> ip.getProduct().getCategory().getCategoryName())
                        .distinct()
                        .collect(Collectors.toList());

        return InvoiceResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .uploadedByUser(invoice.getUploadedByUser().getUsername())
                .invoiceDate(invoice.getInvoiceDate())
                .fileType(invoice.getFileType())
                .fileName(invoice.getFileName())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .productNames(productNames)
                .categories(categories)
                .build();
    }

    private InvoiceDetailsResponse toInvoiceDetailsResponse(Invoice invoice) {
        List<InvoiceDetailsResponse.ProductDetail> productDetails = invoice.getInvoiceProduct() == null ? List.of()
                : invoice.getInvoiceProduct().stream()
                        .map(ip -> InvoiceDetailsResponse.ProductDetail.builder()
                                .productId(ip.getProduct().getProductId())
                                .productName(ip.getProduct().getProductName())
                                .category(ip.getProduct().getCategory().getCategoryName())
                                .unitPrice(ip.getProduct().getUnitPrice())
                                .quantity(ip.getQuantity())
                                .subtotal(ip.getSubtotal().doubleValue())
                                .build())
                        .collect(Collectors.toList());

        List<AuditLogResponse> auditLogs = invoice.getLog() == null ? List.of()
                : invoice.getLog().stream()
                        .map(this::toAuditLogResponse)
                        .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                        .collect(Collectors.toList());

        return InvoiceDetailsResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .uploadedByUser(invoice.getUploadedByUser().getUsername())
                .invoiceDate(invoice.getInvoiceDate())
                .fileType(invoice.getFileType())
                .fileName(invoice.getFileName())
                .originalFileName(invoice.getOriginalFileName())
                .fileSize(invoice.getFileSize())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .products(productDetails)
                .auditLogs(auditLogs)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    private AuditLogResponse toAuditLogResponse(Log log) {
        String description = auditLogService.generateUserFriendlyDescription(log);

        return AuditLogResponse.builder()
                .logId(log.getLogId())
                .performedBy(log.getPerformedBy().getUsername())
                .actionType(log.getActionType())
                .timestamp(log.getTimestamp())
                .oldValues(log.getOldValues())
                .newValues(log.getNewValues())
                .description(description)
                .build();
    }
}