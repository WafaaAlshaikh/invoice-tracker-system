package com.example.invoicetracker.service;

import com.example.invoicetracker.dto.InvoiceRequest;
import com.example.invoicetracker.dto.InvoiceUploadRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceUploadRequestMapperTest {

    @Mock
    private DataProcessingService dataProcessingService;

    @Mock
    private MultipartFile multipartFile;

    private InvoiceUploadRequestMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new InvoiceUploadRequestMapper(dataProcessingService);
    }

    @Nested
    class PrepareInvoiceUploadRequestTests {
        @Test
        void withValidData_allFieldsProvided() {
            // Given
            InvoiceUploadRequest uploadRequest = createUploadRequest(
                    "2025-11-22", "user1", "invoice.pdf", "1,2", "1:2.0,2:3.0", multipartFile);

            when(dataProcessingService.parseProductIds("1,2"))
                    .thenReturn(createMutableList(1L, 2L));

            Map<Long, Double> quantities = createMutableMap(1L, 2.0, 2L, 3.0);
            when(dataProcessingService.parseProductQuantities("1:2.0,2:3.0"))
                    .thenReturn(quantities);

            // When
            InvoiceRequest request = mapper.prepareInvoiceUploadRequest(uploadRequest);

            // Then
            assertNotNull(request);
            assertEquals(LocalDate.of(2025, 11, 22), request.getInvoiceDate());
            assertEquals("user1", request.getUserId());
            assertEquals("invoice.pdf", request.getFileName());
            assertTrue(request.getProductIds().containsAll(Arrays.asList(1L, 2L)));
            assertEquals(quantities, request.getProductQuantities());
            assertEquals(multipartFile, request.getFile());
        }

        @Test
        void withBothProductIdsAndQuantities_mergesCorrectly() {
            // Given
            InvoiceUploadRequest uploadRequest = createUploadRequest(
                    "2025-11-22", "user1", "invoice.pdf", "1,2,3", "1:2.0,2:3.0", multipartFile);

            when(dataProcessingService.parseProductIds("1,2,3"))
                    .thenReturn(createMutableList(1L, 2L, 3L));

            Map<Long, Double> providedQuantities = createMutableMap(1L, 2.0, 2L, 3.0);
            when(dataProcessingService.parseProductQuantities("1:2.0,2:3.0"))
                    .thenReturn(providedQuantities);

            // When
            InvoiceRequest request = mapper.prepareInvoiceUploadRequest(uploadRequest);

            // Then
            assertEquals(3, request.getProductIds().size());
            assertTrue(request.getProductIds().containsAll(Arrays.asList(1L, 2L, 3L)));

            assertEquals(3, request.getProductQuantities().size());
            assertEquals(2.0, request.getProductQuantities().get(1L));
            assertEquals(3.0, request.getProductQuantities().get(2L));
            assertEquals(1.0, request.getProductQuantities().get(3L));
        }

        @Test
        void withProductIdsOnly_setsDefaultQuantities() {
            // Given
            InvoiceUploadRequest uploadRequest = createUploadRequest(
                    "2025-11-22", "user1", "invoice.pdf", "1,2,3", null, multipartFile);

            when(dataProcessingService.parseProductIds("1,2,3"))
                    .thenReturn(createMutableList(1L, 2L, 3L));

            // When
            InvoiceRequest request = mapper.prepareInvoiceUploadRequest(uploadRequest);

            // Then
            assertTrue(request.getProductIds().containsAll(Arrays.asList(1L, 2L, 3L)));
            assertEquals(3, request.getProductIds().size());
            assertEquals(1.0, request.getProductQuantities().get(1L));
            assertEquals(1.0, request.getProductQuantities().get(2L));
            assertEquals(1.0, request.getProductQuantities().get(3L));
        }

        @Test
        void withProductQuantitiesOnly_derivesProductIds() {
            // Given
            InvoiceUploadRequest uploadRequest = createUploadRequest(
                    "2025-11-22", "user1", "invoice.pdf", null, "1:2.0,2:3.0,3:1.5", multipartFile);

            Map<Long, Double> quantities = createMutableMap(1L, 2.0, 2L, 3.0, 3L, 1.5);
            when(dataProcessingService.parseProductQuantities("1:2.0,2:3.0,3:1.5"))
                    .thenReturn(quantities);

            // When
            InvoiceRequest request = mapper.prepareInvoiceUploadRequest(uploadRequest);

            // Then
            assertTrue(request.getProductIds().containsAll(Arrays.asList(1L, 2L, 3L)));
            assertEquals(3, request.getProductIds().size());
            assertEquals(quantities, request.getProductQuantities());
        }

        @Test
        void withProductQuantitiesAndPartialProductIds_mergesBoth() {
            // Given
            InvoiceUploadRequest uploadRequest = createUploadRequest(
                    "2025-11-22", "user1", "invoice.pdf", "3,4", "1:2.0,2:3.0", multipartFile);

            when(dataProcessingService.parseProductIds("3,4"))
                    .thenReturn(createMutableList(3L, 4L));

            Map<Long, Double> providedQuantities = createMutableMap(1L, 2.0, 2L, 3.0);
            when(dataProcessingService.parseProductQuantities("1:2.0,2:3.0"))
                    .thenReturn(providedQuantities);

            // When
            InvoiceRequest request = mapper.prepareInvoiceUploadRequest(uploadRequest);

            // Then
            assertEquals(4, request.getProductIds().size());
            assertTrue(request.getProductIds().containsAll(Arrays.asList(1L, 2L, 3L, 4L)));

            assertEquals(4, request.getProductQuantities().size());
            assertEquals(2.0, request.getProductQuantities().get(1L));
            assertEquals(3.0, request.getProductQuantities().get(2L));
            assertEquals(1.0, request.getProductQuantities().get(3L));
            assertEquals(1.0, request.getProductQuantities().get(4L));
        }

        @Test
        void withEmptyDate_setsCurrentDate() {
            // Given
            InvoiceUploadRequest uploadRequest = createUploadRequest(
                    "", "user1", "invoice.pdf", "1", null, multipartFile);

            when(dataProcessingService.parseProductIds("1"))
                    .thenReturn(createMutableList(1L));

            // When
            InvoiceRequest request = mapper.prepareInvoiceUploadRequest(uploadRequest);

            // Then
            assertEquals(LocalDate.now(), request.getInvoiceDate());
            assertEquals(1, request.getProductIds().size());
            assertTrue(request.getProductIds().contains(1L));
            assertEquals(1.0, request.getProductQuantities().get(1L));
        }

        @Test
        void withNullProductIdsAndQuantities_setsEmptyCollections() {
            // Given
            InvoiceUploadRequest uploadRequest = createUploadRequest(
                    "2025-11-22", "user1", "invoice.pdf", null, null, multipartFile);

            // When
            InvoiceRequest request = mapper.prepareInvoiceUploadRequest(uploadRequest);

            // Then
            assertNotNull(request.getProductIds());
            assertTrue(request.getProductIds().isEmpty());
            assertNotNull(request.getProductQuantities());
            assertTrue(request.getProductQuantities().isEmpty());
        }

        @Test
        void withEmptyProductIdsAndQuantities_setsEmptyCollections() {
            // Given
            InvoiceUploadRequest uploadRequest = createUploadRequest(
                    "2025-11-22", "user1", "invoice.pdf", "", "", multipartFile);

            // When
            InvoiceRequest request = mapper.prepareInvoiceUploadRequest(uploadRequest);

            // Then
            assertNotNull(request.getProductIds());
            assertTrue(request.getProductIds().isEmpty());
            assertNotNull(request.getProductQuantities());
            assertTrue(request.getProductQuantities().isEmpty());
        }
    }

    @Nested
    class ErrorHandlingTests {
        @Test
        void withInvalidDateFormat_throwsException() {
            // Given
            InvoiceUploadRequest uploadRequest = createUploadRequest(
                    "invalid-date", "user1", "invoice.pdf", "1", "1:2.0", multipartFile);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> mapper.prepareInvoiceUploadRequest(uploadRequest));

            assertTrue(exception.getMessage().contains("Invalid date format"));
        }

        @Test
        void withInvalidProductIdsFormat_throwsExceptionFromService() {
            // Given
            InvoiceUploadRequest uploadRequest = createUploadRequest(
                    "2025-11-22", "user1", "invoice.pdf", "invalid-ids", "1:2.0", multipartFile);

            when(dataProcessingService.parseProductIds("invalid-ids"))
                    .thenThrow(new RuntimeException("Invalid product IDs format"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> mapper.prepareInvoiceUploadRequest(uploadRequest));

            assertTrue(exception.getMessage().contains("Invalid product IDs format"));
        }

        @Test
        void withInvalidProductQuantitiesFormat_throwsExceptionFromService() {
            // Given
            InvoiceUploadRequest uploadRequest = createUploadRequest(
                    "2025-11-22", "user1", "invoice.pdf", "1,2", "invalid-quantities", multipartFile);

            when(dataProcessingService.parseProductIds("1,2"))
                    .thenReturn(createMutableList(1L, 2L));

            when(dataProcessingService.parseProductQuantities("invalid-quantities"))
                    .thenThrow(new RuntimeException("Invalid quantities format"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> mapper.prepareInvoiceUploadRequest(uploadRequest));

            assertTrue(exception.getMessage().contains("Invalid quantities format"));
        }
    }

    @Nested
    class PrepareInvoiceUpdateRequestTests {
        @Test
        void withAllFieldsProvided() {
            // Given
            when(dataProcessingService.parseProductIds("1,2"))
                    .thenReturn(createMutableList(1L, 2L));

            Map<Long, Double> quantities = createMutableMap(1L, 2.0, 2L, 3.0);
            when(dataProcessingService.parseProductQuantities("1:2.0,2:3.0"))
                    .thenReturn(quantities);

            // When
            InvoiceRequest request = mapper.prepareInvoiceUpdateRequest(
                    multipartFile, "2025-11-22", "1,2", "1:2.0,2:3.0", "update.pdf");

            // Then
            assertNotNull(request);
            assertEquals(multipartFile, request.getFile());
            assertEquals(LocalDate.of(2025, 11, 22), request.getInvoiceDate());
            assertTrue(request.getProductIds().containsAll(Arrays.asList(1L, 2L)));
            assertEquals(quantities, request.getProductQuantities());
            assertEquals("update.pdf", request.getFileName());
            assertNull(request.getUserId());
        }

        @Test
        void withProductIdsAndQuantities_usesProvidedDataOnly() {
            // Given
            when(dataProcessingService.parseProductIds("1,2,3"))
                    .thenReturn(createMutableList(1L, 2L, 3L));

            Map<Long, Double> providedQuantities = createMutableMap(1L, 2.0, 2L, 3.0);
            when(dataProcessingService.parseProductQuantities("1:2.0,2:3.0"))
                    .thenReturn(providedQuantities);

            // When
            InvoiceRequest request = mapper.prepareInvoiceUpdateRequest(
                    multipartFile, "2025-11-22", "1,2,3", "1:2.0,2:3.0", "update.pdf");

            // Then
            assertEquals(3, request.getProductIds().size());
            assertTrue(request.getProductIds().containsAll(Arrays.asList(1L, 2L, 3L)));
            assertEquals(2.0, request.getProductQuantities().get(1L));
            assertEquals(3.0, request.getProductQuantities().get(2L));
            assertNull(request.getProductQuantities().get(3L));
        }

        @Test
        void withNullValues_setsCollections() {

            // When
            InvoiceRequest request = mapper.prepareInvoiceUpdateRequest(
                    null, null, null, null, null);

            // Then
            assertNull(request.getFile());
            assertNull(request.getInvoiceDate());
            assertNotNull(request.getProductIds());
            assertTrue(request.getProductIds().isEmpty());
            assertNotNull(request.getProductQuantities());
            assertTrue(request.getProductQuantities().isEmpty());
            assertNull(request.getFileName());
            assertNull(request.getUserId());
        }

        @Test
        void withEmptyDate_setsNullDate() {
            // Given
            when(dataProcessingService.parseProductIds("1"))
                    .thenReturn(createMutableList(1L));

            Map<Long, Double> quantities = createMutableMap(1L, 2.0);
            when(dataProcessingService.parseProductQuantities("1:2.0"))
                    .thenReturn(quantities);

            // When
            InvoiceRequest request = mapper.prepareInvoiceUpdateRequest(
                    multipartFile, "", "1", "1:2.0", "test.pdf");

            // Then
            assertNull(request.getInvoiceDate());
            assertEquals(1, request.getProductIds().size());
            assertEquals(2.0, request.getProductQuantities().get(1L));
        }
    }

    @Nested
    class EdgeCasesTests {
        @Test
        void withDuplicateProductIdsInQuantities_usesLastValue() {
            // Given
            InvoiceUploadRequest uploadRequest = createUploadRequest(
                    "2025-11-22", "user1", "invoice.pdf", "1,2", "1:2.0,1:5.0,2:3.0", multipartFile);

            when(dataProcessingService.parseProductIds("1,2"))
                    .thenReturn(createMutableList(1L, 2L));

            Map<Long, Double> quantities = createMutableMap(1L, 5.0, 2L, 3.0);
            when(dataProcessingService.parseProductQuantities("1:2.0,1:5.0,2:3.0"))
                    .thenReturn(quantities);

            // When
            InvoiceRequest request = mapper.prepareInvoiceUploadRequest(uploadRequest);

            // Then
            assertEquals(2, request.getProductIds().size());
            assertEquals(5.0, request.getProductQuantities().get(1L));
            assertEquals(3.0, request.getProductQuantities().get(2L));
        }

        @Test
        void withEmptyQuantitiesButWithProductIds_usesDefaultQuantities() {
            // Given
            InvoiceUploadRequest uploadRequest = createUploadRequest(
                    "2025-11-22", "user1", "invoice.pdf", "1,2,3", "", multipartFile);

            when(dataProcessingService.parseProductIds("1,2,3"))
                    .thenReturn(createMutableList(1L, 2L, 3L));

            // When
            InvoiceRequest request = mapper.prepareInvoiceUploadRequest(uploadRequest);

            // Then
            assertEquals(3, request.getProductIds().size());
            assertEquals(1.0, request.getProductQuantities().get(1L));
            assertEquals(1.0, request.getProductQuantities().get(2L));
            assertEquals(1.0, request.getProductQuantities().get(3L));
        }
    }

    // Helper methods
    private InvoiceUploadRequest createUploadRequest(
            String invoiceDate, String userId, String fileName,
            String productIds, String productQuantities, MultipartFile file) {

        InvoiceUploadRequest uploadRequest = new InvoiceUploadRequest();
        uploadRequest.setInvoiceDate(invoiceDate);
        uploadRequest.setUserId(userId);
        uploadRequest.setFileName(fileName);
        uploadRequest.setProductIds(productIds);
        uploadRequest.setProductQuantities(productQuantities);
        uploadRequest.setFile(file);
        return uploadRequest;
    }

    private List<Long> createMutableList(Long... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }

    private Map<Long, Double> createMutableMap(Object... keyValuePairs) {
        Map<Long, Double> map = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put((Long) keyValuePairs[i], (Double) keyValuePairs[i + 1]);
        }
        return map;
    }
}