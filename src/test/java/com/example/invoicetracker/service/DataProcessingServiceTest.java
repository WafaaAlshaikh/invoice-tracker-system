package com.example.invoicetracker.service;

import com.example.invoicetracker.model.enums.FileType;
import com.example.invoicetracker.model.enums.InvoiceStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataProcessingServiceTest {

    private ObjectMapper objectMapper;
    private DataProcessingService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new DataProcessingService(objectMapper);
    }

    // ------------------- parseProductIds -------------------
    @Test
    void parseProductIds_shouldReturnEmptyList_whenInputIsNullOrEmpty() {
        assertTrue(service.parseProductIds(null).isEmpty());
        assertTrue(service.parseProductIds("").isEmpty());
        assertTrue(service.parseProductIds("   ").isEmpty());
    }

    @Test
    void parseProductIds_shouldParseJsonArray() {
        String input = "[1,2,3]";
        List<Long> result = service.parseProductIds(input);
        assertEquals(List.of(1L, 2L, 3L), result);
    }

    @Test
    void parseProductIds_shouldParseCommaSeparated() {
        String input = "1, 2 ,3";
        List<Long> result = service.parseProductIds(input);
        assertEquals(List.of(1L, 2L, 3L), result);
    }

    @Test
    void parseProductIds_shouldThrowRuntimeException_onInvalidInput() {
        String invalid = "abc,1,2";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.parseProductIds(invalid));
        assertTrue(ex.getMessage().contains("Invalid productIds format"));
    }

    // ------------------- parseProductQuantities -------------------
    @Test
    void parseProductQuantities_shouldReturnEmptyMap_whenInputIsNullOrEmpty() {
        assertTrue(service.parseProductQuantities(null).isEmpty());
        assertTrue(service.parseProductQuantities("").isEmpty());
        assertTrue(service.parseProductQuantities("   ").isEmpty());
    }

    @Test
    void parseProductQuantities_shouldParseJsonMap() {
        String json = "{\"1\":2.0,\"3\":4.5}";
        Map<Long, Double> result = service.parseProductQuantities(json);
        assertEquals(2, result.size());
        assertEquals(2.0, result.get(1L));
        assertEquals(4.5, result.get(3L));
    }

    @Test
    void parseProductQuantities_shouldParseKeyValuePairs() {
        String input = "1:2.0, 3:4.5";
        Map<Long, Double> result = service.parseProductQuantities(input);
        assertEquals(2, result.size());
        assertEquals(2.0, result.get(1L));
        assertEquals(4.5, result.get(3L));
    }

    @Test
    void parseProductQuantities_shouldThrowRuntimeException_onInvalidInput() {
        String invalid = "1:abc,2:3.0";
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.parseProductQuantities(invalid));
        assertTrue(ex.getMessage().contains("Invalid productQuantities format"));
    }

    // ------------------- convertToFileType -------------------
    @Test
    void convertToFileType_shouldReturnEnum_whenValidInput() {
        assertEquals(FileType.PDF, service.convertToFileType("pdf"));
        assertEquals(FileType.IMAGE, service.convertToFileType("IMAGE"));
    }

    @Test
    void convertToFileType_shouldReturnNull_whenInvalidOrNull() {
        assertNull(service.convertToFileType("txt"));
        assertNull(service.convertToFileType(null));
    }

    // ------------------- convertToInvoiceStatus -------------------
    @Test
    void convertToInvoiceStatus_shouldReturnEnum_whenValidInput() {
        assertEquals(InvoiceStatus.COMPLETED, service.convertToInvoiceStatus("COMPLETED"));
        assertEquals(InvoiceStatus.PENDING, service.convertToInvoiceStatus("PENDING"));
    }

    @Test
    void convertToInvoiceStatus_shouldReturnNull_whenInvalidOrNull() {
        assertNull(service.convertToInvoiceStatus("unknown"));
        assertNull(service.convertToInvoiceStatus(null));
    }
}
