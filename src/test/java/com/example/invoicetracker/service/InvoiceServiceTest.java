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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    private InvoiceExtractorService invoiceExtractorService;
    private InvoiceRepository invoiceRepository;
    private UserRepository userRepository;
    private ProductRepository productRepository;
    private InvoiceFactory invoiceFactory;
    private FileStorageService fileStorageService;
    private AuditLogService auditLogService;
    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceExtractorService = mock(InvoiceExtractorService.class);
        invoiceRepository = mock(InvoiceRepository.class);
        userRepository = mock(UserRepository.class);
        productRepository = mock(ProductRepository.class);
        invoiceFactory = mock(InvoiceFactory.class);
        fileStorageService = mock(FileStorageService.class);
        auditLogService = mock(AuditLogService.class);

        invoiceService = new InvoiceService(
                invoiceExtractorService,
                invoiceRepository,
                userRepository,
                productRepository,
                invoiceFactory,
                fileStorageService,
                auditLogService);
    }

    @Nested
    class CreateInvoiceTests {

        @Test
        void createInvoice_withFileAndProducts_andAISuccess() throws Exception {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getContentType()).thenReturn("application/pdf");
            when(file.getOriginalFilename()).thenReturn("invoice.pdf");

            Map<Long, Double> productQuantities = Map.of(1L, 2.0);
            InvoiceRequest request = new InvoiceRequest();
            request.setFile(file);
            request.setProductQuantities(productQuantities);

            InvoiceExtractionResult extractionResult = new InvoiceExtractionResult();
            extractionResult.setSuccess(true);
            extractionResult.setTotalAmount(150.0);
            when(invoiceExtractorService.extractInvoiceData(file)).thenReturn(extractionResult);

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(true)
                    .invoiceProduct(new ArrayList<>())
                    .build();
            when(invoiceFactory.createInvoice(request, user)).thenReturn(invoice);
            when(invoiceRepository.save(invoice)).thenReturn(invoice);

            // Act
            InvoiceResponse response = invoiceService.createInvoice(request, "user1", "USER");

            // Assert
            assertEquals(150.0, response.getTotalAmount());
            assertEquals(1L, response.getInvoiceId());
            verify(auditLogService).logInvoiceAction(invoice, user, ActionType.CREATE, null, null);
        }

        @Test
        void createInvoice_withProductsOnly_fallbackCalculation() throws Exception {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

            Product product = Product.builder()
                    .productId(1L)
                    .productName("Product1")
                    .unitPrice(50.0)
                    .category(Category.builder().categoryName("Cat1").build())
                    .isActive(true)
                    .build();

            InvoiceRequest request = new InvoiceRequest();
            request.setProductQuantities(Map.of(1L, 2.0));

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(true)
                    .invoiceProduct(new ArrayList<>())
                    .build();

            when(invoiceFactory.createInvoice(request, user)).thenReturn(invoice);
            when(invoiceRepository.save(invoice)).thenReturn(invoice);
            when(productRepository.findAllByIdAndIsActiveTrue(Set.of(1L))).thenReturn(List.of(product));

            // Act
            InvoiceResponse response = invoiceService.createInvoice(request, "user1", "USER");

            // Assert
            assertEquals(100.0, response.getTotalAmount());
            verify(auditLogService).logInvoiceAction(invoice, user, ActionType.CREATE, null, null);
        }

        @Test
        void createInvoice_withFileOnly_andAIFailure_usesFallback() throws Exception {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getContentType()).thenReturn("application/pdf");

            InvoiceRequest request = new InvoiceRequest();
            request.setFile(file);

            InvoiceExtractionResult extractionResult = new InvoiceExtractionResult();
            extractionResult.setSuccess(false);
            extractionResult.setErrorMessage("Extraction failed");
            when(invoiceExtractorService.extractInvoiceData(file)).thenReturn(extractionResult);

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(true)
                    .invoiceProduct(new ArrayList<>())
                    .totalAmount(0.0)
                    .build();
            when(invoiceFactory.createInvoice(request, user)).thenReturn(invoice);
            when(invoiceRepository.save(invoice)).thenReturn(invoice);

            // Act
            InvoiceResponse response = invoiceService.createInvoice(request, "user1", "USER");

            // Assert
            assertEquals(0.0, response.getTotalAmount());
        }

        // @Test
        // void createInvoice_withFileOnly_andAIThrowsException_usesFallback() throws
        // Exception {
        // // Arrange
        // User user = User.builder().username("user1").isActive(true).build();
        // when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

        // MultipartFile file = mock(MultipartFile.class);
        // when(file.isEmpty()).thenReturn(false);
        // when(file.getContentType()).thenReturn("application/pdf");

        // InvoiceRequest request = new InvoiceRequest();
        // request.setFile(file);

        // when(invoiceExtractorService.extractInvoiceData(file)).thenThrow(new
        // Exception("AI Service unavailable"));

        // Invoice invoice = Invoice.builder()
        // .invoiceId(1L)
        // .uploadedByUser(user)
        // .isActive(true)
        // .invoiceProduct(new ArrayList<>())
        // .totalAmount(0.0)
        // .build();
        // when(invoiceFactory.createInvoice(request, user)).thenReturn(invoice);
        // when(invoiceRepository.save(invoice)).thenReturn(invoice);

        // // Act
        // InvoiceResponse response = invoiceService.createInvoice(request, "user1",
        // "USER");

        // // Assert
        // assertEquals(0.0, response.getTotalAmount());
        // }

        @Test
        void createInvoice_invalidRequest_noFileNoProducts_throwsException() {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

            InvoiceRequest request = new InvoiceRequest();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> invoiceService.createInvoice(request, "user1", "USER"));

            assertTrue(exception.getMessage().contains("Invoice must have either a file or product information"));
        }

        @Test
        void createInvoice_superuserForOtherUser_success() throws Exception {
            // Arrange
            User currentUser = User.builder().username("admin").isActive(true).build();
            User targetUser = User.builder().userId("user123").username("targetUser").isActive(true).build();

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(currentUser));
            when(userRepository.findById("user123")).thenReturn(Optional.of(targetUser));

            Product product = Product.builder()
                    .productId(1L)
                    .productName("Test Product")
                    .unitPrice(50.0)
                    .category(Category.builder().categoryName("Electronics").build())
                    .isActive(true)
                    .build();

            InvoiceRequest request = new InvoiceRequest();
            request.setProductQuantities(Map.of(1L, 2.0));
            request.setUserId("user123");
            request.setInvoiceDate(LocalDate.now());
            request.setFileName("test_invoice.pdf");

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(targetUser)
                    .isActive(true)
                    .totalAmount(100.0) 
                    .invoiceDate(LocalDate.now())
                    .fileName("test_invoice.pdf")
                    .fileType(FileType.WEB_FORM)
                    .invoiceProduct(new ArrayList<>())
                    .build();

            when(invoiceFactory.createInvoice(request, targetUser)).thenReturn(invoice);
            when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);
            when(productRepository.findAllByIdAndIsActiveTrue(Set.of(1L))).thenReturn(List.of(product));

            // Act
            InvoiceResponse response = invoiceService.createInvoice(request, "admin", "SUPERUSER");

            // Assert
            assertNotNull(response);
            assertEquals(1L, response.getInvoiceId());
            assertEquals("targetUser", response.getUploadedByUser());
            assertEquals(100.0, response.getTotalAmount());
            verify(invoiceFactory).createInvoice(request, targetUser);
            verify(auditLogService).logInvoiceAction(any(Invoice.class), eq(currentUser), eq(ActionType.CREATE),
                    isNull(), isNull());
        }

        @Test
        void createInvoice_superuserForInactiveUser_throwsException() {
            // Arrange
            User currentUser = User.builder().username("admin").isActive(true).build();
            User targetUser = User.builder().userId("user123").username("targetUser").isActive(false).build();

            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(currentUser));
            when(userRepository.findById("user123")).thenReturn(Optional.of(targetUser));

            InvoiceRequest request = new InvoiceRequest();
            request.setProductQuantities(Map.of(1L, 2.0));
            request.setUserId("user123");

            // Act & Assert
            AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                    () -> invoiceService.createInvoice(request, "admin", "SUPERUSER"));

            assertTrue(exception.getMessage().contains("Cannot create invoice for inactive user"));
        }

        @Test
        void createInvoice_unsupportedRole_throwsAccessDenied() {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

            InvoiceRequest request = new InvoiceRequest();
            request.setProductQuantities(Map.of(1L, 2.0));

            // Act & Assert
            assertThrows(AccessDeniedException.class,
                    () -> invoiceService.createInvoice(request, "user1", "GUEST"));
        }
    }

    @Nested
    class UpdateInvoiceTests {

        @Test
        void updateInvoice_superuser_canChangeStatus() {
            // Arrange
            User user = User.builder().username("admin").isActive(true).build();
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(true)
                    .status(InvoiceStatus.PENDING)
                    .totalAmount(100.0)
                    .invoiceProduct(new ArrayList<>())
                    .build();
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(invoice)).thenReturn(invoice);
            when(auditLogService.captureInvoiceState(invoice)).thenReturn(new HashMap<>());

            InvoiceRequest request = new InvoiceRequest();
            request.setStatus(InvoiceStatus.APPROVED);

            // Act
            InvoiceResponse response = invoiceService.updateInvoice(1L, request, "admin", "SUPERUSER");

            // Assert
            assertEquals(InvoiceStatus.APPROVED, response.getStatus());
            verify(auditLogService).logInvoiceAction(any(), any(), eq(ActionType.UPDATE), any(), any());
        }

        @Test
        void updateInvoice_withNewFileAndAIExtraction_success() throws Exception {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(true)
                    .totalAmount(100.0)
                    .invoiceProduct(new ArrayList<>())
                    .build();
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(invoice)).thenReturn(invoice);
            when(auditLogService.captureInvoiceState(invoice)).thenReturn(new HashMap<>());

            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getContentType()).thenReturn("application/pdf");

            InvoiceRequest request = new InvoiceRequest();
            request.setFile(file);

            InvoiceExtractionResult extractionResult = new InvoiceExtractionResult();
            extractionResult.setSuccess(true);
            extractionResult.setTotalAmount(200.0);
            when(invoiceExtractorService.extractInvoiceData(file)).thenReturn(extractionResult);

            // Act
            InvoiceResponse response = invoiceService.updateInvoice(1L, request, "user1", "USER");

            // Assert
            assertEquals(200.0, response.getTotalAmount());
        }

        @Test
        void updateInvoice_withNewFileAndAIThrowsException_usesExistingTotal() throws Exception {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(true)
                    .totalAmount(100.0)
                    .invoiceProduct(new ArrayList<>())
                    .build();
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(invoice)).thenReturn(invoice);
            when(auditLogService.captureInvoiceState(invoice)).thenReturn(new HashMap<>());

            MultipartFile file = mock(MultipartFile.class);
            when(file.isEmpty()).thenReturn(false);
            when(file.getContentType()).thenReturn("application/pdf");

            InvoiceRequest request = new InvoiceRequest();
            request.setFile(file);

            when(invoiceExtractorService.extractInvoiceData(file)).thenThrow(new RuntimeException("AI Service down"));

            // Act
            InvoiceResponse response = invoiceService.updateInvoice(1L, request, "user1", "USER");

            // Assert
            assertEquals(100.0, response.getTotalAmount());
        }

        @Test
        void updateInvoice_withProducts_calculatesTotal() {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(true)
                    .totalAmount(100.0)
                    .invoiceProduct(new ArrayList<>())
                    .build();
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(invoice)).thenReturn(invoice);
            when(auditLogService.captureInvoiceState(invoice)).thenReturn(new HashMap<>());

            Product product = Product.builder()
                    .productId(1L)
                    .unitPrice(50.0)
                    .category(Category.builder().categoryName("Electronics").build())
                    .isActive(true)
                    .build();
            when(productRepository.findAllByIdAndIsActiveTrue(Set.of(1L))).thenReturn(List.of(product));

            InvoiceRequest request = new InvoiceRequest();
            request.setProductQuantities(Map.of(1L, 3.0));

            // Act
            InvoiceResponse response = invoiceService.updateInvoice(1L, request, "user1", "USER");

            // Assert
            assertEquals(150.0, response.getTotalAmount());
        }

        @Test
        void updateInvoice_withFileName_updatesFileName() {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(true)
                    .totalAmount(100.0)
                    .fileName("old_name.pdf")
                    .invoiceProduct(new ArrayList<>())
                    .build();
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(invoice)).thenReturn(invoice);
            when(auditLogService.captureInvoiceState(invoice)).thenReturn(new HashMap<>());

            InvoiceRequest request = new InvoiceRequest();
            request.setFileName("new_name.pdf");

            // Act
            InvoiceResponse response = invoiceService.updateInvoice(1L, request, "user1", "USER");

            // Assert
            assertNotNull(response);
            verify(invoiceRepository).save(invoice);
        }

        @Test
        void updateInvoice_unauthorizedUser_throwsAccessDenied() {
            // Arrange
            User owner = User.builder().username("owner").isActive(true).build();
            User otherUser = User.builder().username("other").isActive(true).build();

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(owner)
                    .isActive(true)
                    .build();
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));

            InvoiceRequest request = new InvoiceRequest();

            // Act & Assert
            assertThrows(AccessDeniedException.class,
                    () -> invoiceService.updateInvoice(1L, request, "other", "USER"));
        }
    }

    @Nested
    class DeleteInvoiceTests {

        @Test
        void deleteInvoice_setsIsActiveFalse() {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(true)
                    .build();
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(invoice)).thenReturn(invoice);

            // Act
            invoiceService.deleteInvoice(1L, "user1", "USER");

            // Assert
            assertFalse(invoice.getIsActive());
            verify(auditLogService).logInvoiceAction(invoice, user, ActionType.DELETE, null, null);
        }

        // @Test
        // void deleteInvoice_unauthorizedUser_throwsAccessDenied() {
        // // Arrange
        // User owner = User.builder().username("owner").isActive(true).build();
        // User otherUser = User.builder().username("other").isActive(true).build();

        // Invoice invoice = Invoice.builder()
        // .invoiceId(1L)
        // .uploadedByUser(owner)
        // .isActive(true)
        // .build();
        // when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        // when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));

        // // Act & Assert
        // assertThrows(AccessDeniedException.class,
        // () -> invoiceService.deleteInvoice(1L, "other", "USER"));
        // }
    }

    @Nested
    class GetInvoiceTests {

        @Test
        void getInvoiceById_success() {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(true)
                    .fileType(FileType.PDF)
                    .fileName("invoice.pdf")
                    .totalAmount(100.0)
                    .status(InvoiceStatus.PENDING)
                    .invoiceDate(LocalDate.now())
                    .invoiceProduct(new ArrayList<>())
                    .build();
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

            // Act
            InvoiceResponse response = invoiceService.getInvoiceById(1L, "user1", "USER");

            // Assert
            assertNotNull(response);
            assertEquals(1L, response.getInvoiceId());
            assertEquals("user1", response.getUploadedByUser());
        }

        @Test
        void getInvoiceById_accessDenied_forDifferentUser() {
            // Arrange
            User owner = User.builder().username("owner").isActive(true).build();
            User otherUser = User.builder().username("otherUser").isActive(true).build();

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(owner)
                    .isActive(true)
                    .fileType(FileType.PDF)
                    .fileName("invoice.pdf")
                    .totalAmount(100.0)
                    .status(InvoiceStatus.PENDING)
                    .invoiceDate(LocalDate.now())
                    .invoiceProduct(new ArrayList<>())
                    .build();
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
           

            // Act & Assert
            assertThrows(AccessDeniedException.class,
                    () -> invoiceService.getInvoiceById(1L, "otherUser", "USER"));
        }

        @Test
        void getInvoiceById_auditorRole_canAccess() {
            // Arrange
            User owner = User.builder().username("owner").isActive(true).build();

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(owner)
                    .isActive(true)
                    .fileType(FileType.PDF)
                    .fileName("invoice.pdf")
                    .totalAmount(100.0)
                    .status(InvoiceStatus.PENDING)
                    .invoiceDate(LocalDate.now())
                    .invoiceProduct(new ArrayList<>())
                    .build();
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

            // Act
            InvoiceResponse response = invoiceService.getInvoiceById(1L, "auditor", "AUDITOR");

            // Assert
            assertNotNull(response);
            assertEquals(1L, response.getInvoiceId());
            assertEquals("owner", response.getUploadedByUser());
        }

        @Test
        void getInvoiceById_superuserRole_canAccess() {
            // Arrange
            User owner = User.builder().username("owner").isActive(true).build();
          
            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(owner)
                    .isActive(true)
                    .fileType(FileType.PDF)
                    .fileName("invoice.pdf")
                    .totalAmount(100.0)
                    .status(InvoiceStatus.PENDING)
                    .invoiceDate(LocalDate.now())
                    .invoiceProduct(new ArrayList<>())
                    .build();
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

            // Act
            InvoiceResponse response = invoiceService.getInvoiceById(1L, "superuser", "SUPERUSER");

            // Assert
            assertNotNull(response);
            assertEquals(1L, response.getInvoiceId());
            assertEquals("owner", response.getUploadedByUser());
        }

        @Test
        void getAllInvoices_returnsPage() {
            // Arrange
            User user = User.builder().username("user1").build();
            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(true)
                    .fileType(FileType.PDF)
                    .fileName("invoice.pdf")
                    .totalAmount(100.0)
                    .status(InvoiceStatus.PENDING)
                    .invoiceDate(LocalDate.now())
                    .invoiceProduct(new ArrayList<>())
                    .build();

            Page<Invoice> invoicePage = new PageImpl<>(List.of(invoice));
            when(invoiceRepository.findAllByIsActiveTrue(any(Pageable.class))).thenReturn(invoicePage);

            // Act
            Page<InvoiceResponse> result = invoiceService.getAllInvoices(PageRequest.of(0, 10));

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }

        @Test
        void getUserInvoices_returnsUserInvoices() {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user)); 

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(true)
                    .fileType(FileType.PDF)
                    .fileName("invoice.pdf")
                    .totalAmount(100.0)
                    .status(InvoiceStatus.PENDING)
                    .invoiceDate(LocalDate.now())
                    .invoiceProduct(new ArrayList<>())
                    .build();

            Page<Invoice> invoicePage = new PageImpl<>(List.of(invoice));
            when(invoiceRepository.findAllByUploadedByUserAndIsActiveTrue(eq(user), any(Pageable.class)))
                    .thenReturn(invoicePage);

            // Act
            Page<InvoiceResponse> result = invoiceService.getUserInvoices("user1", PageRequest.of(0, 10));

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }
    }

    @Nested
    class SearchInvoiceTests {

        @Test
        void searchInvoices_noCriteria_userRole_callsRepository() {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

            Page<Invoice> emptyPage = new PageImpl<>(List.of());
            when(invoiceRepository.findAllByUploadedByUserAndIsActiveTrue(eq(user), any(Pageable.class)))
                    .thenReturn(emptyPage);

            InvoiceSearchRequest searchRequest = new InvoiceSearchRequest();
            searchRequest.setPage(0);
            searchRequest.setSize(10);
            searchRequest.setSortBy("invoiceId");
            searchRequest.setDirection("ASC");

            // Act
            Page<InvoiceResponse> result = invoiceService.searchInvoices(searchRequest, "user1", "USER");

            // Assert
            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
        }

        @Test
        void searchInvoices_withCriteria_superuser_callsSearchRepository() {
            // Arrange
           

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(User.builder().username("admin").build())
                    .isActive(true)
                    .fileType(FileType.PDF)
                    .fileName("invoice.pdf")
                    .totalAmount(100.0)
                    .status(InvoiceStatus.PENDING)
                    .invoiceDate(LocalDate.now())
                    .invoiceProduct(new ArrayList<>())
                    .build();

            Page<Invoice> invoicePage = new PageImpl<>(List.of(invoice));
            when(invoiceRepository.searchAllInvoices(
                    any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(invoicePage);

            InvoiceSearchRequest searchRequest = new InvoiceSearchRequest();
            searchRequest.setPage(0);
            searchRequest.setSize(10);
            searchRequest.setSortBy("invoiceId");
            searchRequest.setDirection("ASC");
            searchRequest.setSearch("invoice");
            searchRequest.setFileType("PDF");
            searchRequest.setStatus("PENDING");

            // Act
            Page<InvoiceResponse> result = invoiceService.searchInvoices(searchRequest, "admin", "SUPERUSER");

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(invoiceRepository).searchAllInvoices(any(), any(), any(), any(), any(), any(Pageable.class));
        }

        @Test
        void searchInvoices_withCriteria_userRole_callsUserSearchRepository() {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(true)
                    .fileType(FileType.PDF)
                    .fileName("invoice.pdf")
                    .totalAmount(100.0)
                    .status(InvoiceStatus.PENDING)
                    .invoiceDate(LocalDate.now())
                    .invoiceProduct(new ArrayList<>())
                    .build();

            Page<Invoice> invoicePage = new PageImpl<>(List.of(invoice));
            when(invoiceRepository.searchUserInvoices(
                    eq(user), any(), any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(invoicePage);

            InvoiceSearchRequest searchRequest = new InvoiceSearchRequest();
            searchRequest.setPage(0);
            searchRequest.setSize(10);
            searchRequest.setSortBy("invoiceId");
            searchRequest.setDirection("ASC");
            searchRequest.setSearch("invoice");
            searchRequest.setStartDate(LocalDate.now().minusDays(7));
            searchRequest.setEndDate(LocalDate.now());

            // Act
            Page<InvoiceResponse> result = invoiceService.searchInvoices(searchRequest, "user1", "USER");

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }
    }

    @Nested
    class StatusUpdateTests {

        @Test
        void updateInvoiceStatus_onlySuperuserAllowed() {
            // Arrange
            User user = User.builder().username("admin").isActive(true).build();
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(true)
                    .status(InvoiceStatus.PENDING)
                    .build();
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(invoice)).thenReturn(invoice);

            // Act
            InvoiceResponse response = invoiceService.updateInvoiceStatus(1L, "APPROVED", "admin", "SUPERUSER");

            // Assert
            assertEquals(InvoiceStatus.APPROVED, response.getStatus());
            verify(auditLogService).logStatusChange(invoice, user, InvoiceStatus.PENDING, InvoiceStatus.APPROVED);
        }

        @Test
        void updateInvoiceStatus_nonSuperuser_throwsAccessDenied() {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(true)
                    .status(InvoiceStatus.PENDING)
                    .build();
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

            // Act & Assert
            assertThrows(AccessDeniedException.class,
                    () -> invoiceService.updateInvoiceStatus(1L, "APPROVED", "user1", "USER"));

            verify(invoiceRepository, never()).save(any(Invoice.class));
            verify(auditLogService, never()).logStatusChange(any(), any(), any(), any());
        }

        @Test
        void updateInvoiceStatus_invalidStatus_throwsException() {
            // Arrange
            User user = User.builder().username("admin").isActive(true).build();
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(true)
                    .status(InvoiceStatus.PENDING)
                    .build();
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> invoiceService.updateInvoiceStatus(1L, "INVALID_STATUS", "admin", "SUPERUSER"));
        }
    }

    @Nested
    class StatsTests {

        @Test
        void getInvoiceStats_userRole_returnsUserStats() {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();
            when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

            when(invoiceRepository.countByUploadedByUserAndIsActiveTrue(user)).thenReturn(2L);
            when(invoiceRepository.sumTotalAmountByUserAndIsActiveTrue(user)).thenReturn(200.0);

            // Act
            Map<String, Object> stats = invoiceService.getInvoiceStats("user1", "USER");

            // Assert
            assertEquals(2L, stats.get("totalInvoices"));
            assertEquals(200.0, stats.get("totalAmount"));
            assertEquals(100.0, stats.get("averageAmount"));
        }

        @Test
        void getInvoiceStats_adminRole_returnsAdminStats() {
            // Arrange
            User adminUser = User.builder().username("admin").isActive(true).build();
            when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

            when(invoiceRepository.countByIsActiveTrue()).thenReturn(5L);
            when(invoiceRepository.sumTotalAmountByIsActiveTrue()).thenReturn(500.0);
            when(userRepository.countByIsActiveTrue()).thenReturn(3L);

            // Act
            Map<String, Object> stats = invoiceService.getInvoiceStats("admin", "ADMIN");

            // Assert
            assertEquals(5L, stats.get("totalInvoices"));
            assertEquals(500.0, stats.get("totalAmount"));
            assertEquals(3L, stats.get("totalUsers"));
            assertEquals(100.0, stats.get("averageAmount"));
        }

        @Test
        void getInvoiceStats_superuserRole_returnsAdminStats() {
            // Arrange
            User superuser = User.builder().username("superuser").isActive(true).build();
            when(userRepository.findByUsername("superuser")).thenReturn(Optional.of(superuser));

            when(invoiceRepository.countByIsActiveTrue()).thenReturn(10L);
            when(invoiceRepository.sumTotalAmountByIsActiveTrue()).thenReturn(1000.0);
            when(userRepository.countByIsActiveTrue()).thenReturn(5L);

            // Act
            Map<String, Object> stats = invoiceService.getInvoiceStats("superuser", "SUPERUSER");

            // Assert
            assertEquals(10L, stats.get("totalInvoices"));
            assertEquals(1000.0, stats.get("totalAmount"));
            assertEquals(5L, stats.get("totalUsers"));
        }
    }

    // @Nested
    // class FileDownloadTests {

    // // @Test
    // // void downloadInvoiceFile_success() throws Exception {
    // // // Arrange
    // // User user = User.builder().username("user1").isActive(true).build();
    // //
    // when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

    // // Invoice invoice = Invoice.builder()
    // // .invoiceId(1L)
    // // .uploadedByUser(user)
    // // .isActive(true)
    // // .fileType(FileType.PDF)
    // // .storedFileName("stored_file.pdf")
    // // .originalFileName("invoice.pdf")
    // // .build();
    // // when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

    // // byte[] fileContent = "file content".getBytes();
    // //
    // when(fileStorageService.loadFile("stored_file.pdf")).thenReturn(fileContent);
    // //
    // when(fileStorageService.determineContentType(FileType.PDF)).thenReturn("application/pdf");
    // // when(fileStorageService.createFileResponse(any(), any(), any()))
    // // .thenReturn(ResponseEntity.ok().body(fileContent));

    // // // Act
    // // ResponseEntity<byte[]> response = invoiceService.downloadInvoiceFile(1L,
    // "user1", "USER");

    // // // Assert
    // // assertNotNull(response);
    // // }

    // // @Test
    // // void downloadInvoiceFile_webFormType_throwsException() {
    // // // Arrange
    // // User user = User.builder().username("user1").isActive(true).build();
    // //
    // when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

    // // Invoice invoice = Invoice.builder()
    // // .invoiceId(1L)
    // // .uploadedByUser(user)
    // // .isActive(true)
    // // .fileType(FileType.WEB_FORM)
    // // .build();
    // // when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

    // // // Act & Assert
    // // assertThrows(ResourceNotFoundException.class,
    // // () -> invoiceService.downloadInvoiceFile(1L, "user1", "USER"));
    // // }

    // // @Test
    // // void downloadInvoiceFile_noStoredFileName_throwsException() {
    // // // Arrange
    // // User user = User.builder().username("user1").isActive(true).build();
    // //
    // when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

    // // Invoice invoice = Invoice.builder()
    // // .invoiceId(1L)
    // // .uploadedByUser(user)
    // // .isActive(true)
    // // .fileType(FileType.PDF)
    // // .storedFileName(null)
    // // .build();
    // // when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

    // // // Act & Assert
    // // assertThrows(ResourceNotFoundException.class,
    // // () -> invoiceService.downloadInvoiceFile(1L, "user1", "USER"));
    // // }

    // // @Test
    // // void downloadInvoiceFile_fileStorageException_throwsException() throws
    // Exception {
    // // // Arrange
    // // User user = User.builder().username("user1").isActive(true).build();
    // //
    // when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

    // // Invoice invoice = Invoice.builder()
    // // .invoiceId(1L)
    // // .uploadedByUser(user)
    // // .isActive(true)
    // // .fileType(FileType.PDF)
    // // .storedFileName("stored_file.pdf")
    // // .originalFileName("invoice.pdf")
    // // .build();
    // // when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

    // // when(fileStorageService.loadFile("stored_file.pdf")).thenThrow(new
    // RuntimeException("File not found"));

    // // // Act & Assert
    // // assertThrows(ResourceNotFoundException.class,
    // // () -> invoiceService.downloadInvoiceFile(1L, "user1", "USER"));
    // // }
    // }

    // @Nested
    // class DetailsAndAuditTests {

    // @Test
    // void getInvoiceDetails_success() {
    // // Arrange
    // User user = User.builder().username("user1").isActive(true).build();
    // when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

    // Product product = Product.builder()
    // .productId(1L)
    // .productName("Product1")
    // .category(Category.builder().categoryName("Cat1").build())
    // .unitPrice(50.0)
    // .build();

    // InvoiceProduct invoiceProduct = InvoiceProduct.builder()
    // .product(product)
    // .quantity(2.0)
    // .unitPrice(BigDecimal.valueOf(50.0))
    // .subtotal(BigDecimal.valueOf(100.0))
    // .build();

    // Invoice invoice = Invoice.builder()
    // .invoiceId(1L)
    // .uploadedByUser(user)
    // .isActive(true)
    // .fileType(FileType.PDF)
    // .fileName("invoice.pdf")
    // .originalFileName("invoice.pdf")
    // .fileSize(1024L)
    // .totalAmount(100.0)
    // .status(InvoiceStatus.PENDING)
    // .invoiceDate(LocalDate.now())
    // .createdAt(LocalDateTime.now())
    // .updatedAt(LocalDateTime.now())
    // .invoiceProduct(List.of(invoiceProduct))
    // .log(new ArrayList<>())
    // .build();

    // when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

    // // Act
    // InvoiceDetailsResponse response = invoiceService.getInvoiceDetails(1L,
    // "user1", "USER");

    // // Assert
    // assertNotNull(response);
    // assertEquals(1L, response.getInvoiceId());
    // assertEquals(1, response.getProducts().size());
    // }

    // @Test
    // void getInvoiceAuditLogs_success() {
    // // Arrange
    // User user = User.builder().username("user1").isActive(true).build();
    // User performer = User.builder().username("admin").isActive(true).build();

    // when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

    // Log auditLog = Log.builder()
    // .logId(1L)
    // .performedBy(performer)
    // .actionType(ActionType.CREATE)
    // .timestamp(LocalDateTime.now())
    // .build();

    // Invoice invoice = Invoice.builder()
    // .invoiceId(1L)
    // .uploadedByUser(user)
    // .isActive(true)
    // .log(List.of(auditLog))
    // .build();

    // when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
    // when(auditLogService.generateUserFriendlyDescription(auditLog)).thenReturn("Invoice
    // created");

    // // Act
    // List<AuditLogResponse> auditLogs = invoiceService.getInvoiceAuditLogs(1L,
    // "user1", "USER");

    // // Assert
    // assertNotNull(auditLogs);
    // assertEquals(1, auditLogs.size());
    // }
    // }

    @Nested
    class HelperMethodTests {

        @Test
        void getActiveInvoice_notFound_throwsException() {
            // Arrange
            when(invoiceRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> invoiceService.getInvoiceById(1L, "user1", "USER"));
        }

        @Test
        void getActiveInvoice_inactive_throwsException() {
            // Arrange
            User user = User.builder().username("user1").isActive(true).build();
            Invoice invoice = Invoice.builder()
                    .invoiceId(1L)
                    .uploadedByUser(user)
                    .isActive(false)
                    .build();
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> invoiceService.getInvoiceById(1L, "user1", "USER"));
        }

        @Test
        void getUserByUsername_notFound_throwsException() {
            // Arrange
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> invoiceService.createInvoice(new InvoiceRequest(), "unknown", "USER"));
        }
    }
}