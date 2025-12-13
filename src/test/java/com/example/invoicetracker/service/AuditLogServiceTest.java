package com.example.invoicetracker.service;

import com.example.invoicetracker.model.entity.Invoice;
import com.example.invoicetracker.model.entity.Log;
import com.example.invoicetracker.model.entity.User;
import com.example.invoicetracker.model.enums.ActionType;
import com.example.invoicetracker.model.enums.InvoiceStatus;
import com.example.invoicetracker.model.enums.FileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogServiceTest {

    private AuditLogService auditLogService;

    @BeforeEach
    void setup() {
        auditLogService = new AuditLogService();
    }

    // generateUserFriendlyDescription()
    @Test
    void generateUserFriendlyDescription_createAction() {
        User user = User.builder().username("john").build();
        Log log = Log.builder()
                .performedBy(user)
                .actionType(ActionType.CREATE)
                .build();

        String desc = auditLogService.generateUserFriendlyDescription(log);

        assertEquals("john created this invoice", desc);
    }

    @Test
    void generateUserFriendlyDescription_updateActionWithChanges() {
        User user = User.builder().username("alice").build();
        Log log = Log.builder()
                .performedBy(user)
                .actionType(ActionType.UPDATE)
                .oldValues(Map.of("status", "PENDING", "totalAmount", "100"))
                .newValues(Map.of("status", "APPROVED", "totalAmount", "150"))
                .build();

        String desc = auditLogService.generateUserFriendlyDescription(log);

        assertTrue(desc.contains("alice updated this invoice"));
        assertTrue(desc.contains("status from PENDING to APPROVED"));
        assertTrue(desc.contains("total amount from $100 to $150"));
    }

    @Test
    void generateUserFriendlyDescription_updateActionNoChanges() {
        User user = User.builder().username("bob").build();
        Log log = Log.builder()
                .performedBy(user)
                .actionType(ActionType.UPDATE)
                .oldValues(null)
                .newValues(null)
                .build();

        String desc = auditLogService.generateUserFriendlyDescription(log);

        assertEquals("bob updated this invoice", desc);
    }

    @Test
    void generateUserFriendlyDescription_nullUser() {
        Log log = Log.builder()
                .performedBy(null)
                .actionType(ActionType.CREATE)
                .build();

        assertThrows(NullPointerException.class, () -> {
            auditLogService.generateUserFriendlyDescription(log);
        });
    }

    @Test
    void generateUserFriendlyDescription_emptyMaps() {
        User user = User.builder().username("user").build();
        Log log = Log.builder()
                .performedBy(user)
                .actionType(ActionType.UPDATE)
                .oldValues(Map.of())
                .newValues(Map.of())
                .build();

        String desc = auditLogService.generateUserFriendlyDescription(log);
        assertEquals("user updated this invoice", desc);
    }

    @Test
    void generateUserFriendlyDescription_allActionTypes() {
        User user = User.builder().username("test").build();

        for (ActionType actionType : ActionType.values()) {
            Log log = Log.builder()
                    .performedBy(user)
                    .actionType(actionType)
                    .build();

            String desc = auditLogService.generateUserFriendlyDescription(log);
            assertNotNull(desc);
            assertTrue(desc.contains("test"));
        }
    }

    // logInvoiceAction() & addLogToInvoice()
    @Test
    void logInvoiceAction_addsLogToInvoice() {
        User user = User.builder().username("john").build();
        Invoice invoice = new Invoice();
        assertNull(invoice.getLog());

        auditLogService.logInvoiceAction(invoice, user, ActionType.CREATE, null, null);

        assertNotNull(invoice.getLog());
        assertEquals(1, invoice.getLog().size());
        Log logEntry = invoice.getLog().get(0);
        assertEquals(user, logEntry.getPerformedBy());
        assertEquals(ActionType.CREATE, logEntry.getActionType());
    }

    // logStatusChange()
    @Test
    void logStatusChange_createsStatusUpdateLog() {
        User user = User.builder().username("admin").build();
        Invoice invoice = new Invoice();

        auditLogService.logStatusChange(invoice, user, InvoiceStatus.PENDING, InvoiceStatus.APPROVED);

        assertNotNull(invoice.getLog());
        assertEquals(1, invoice.getLog().size());
        Log logEntry = invoice.getLog().get(0);
        assertEquals(ActionType.UPDATE, logEntry.getActionType());
        assertEquals("PENDING", logEntry.getOldValues().get("status"));
        assertEquals("APPROVED", logEntry.getNewValues().get("status"));
    }

    // captureInvoiceState()
    @Test
    void captureInvoiceState_returnsCorrectMap() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceDate(LocalDate.of(2025, 11, 20));
        invoice.setFileType(FileType.PDF);
        invoice.setFileName("invoice.pdf");
        invoice.setTotalAmount(250.0);
        invoice.setStatus(InvoiceStatus.APPROVED);

        Map<String, Object> state = auditLogService.captureInvoiceState(invoice);

        assertEquals("2025-11-20", state.get("invoiceDate"));
        assertEquals("PDF", state.get("fileType"));
        assertEquals("invoice.pdf", state.get("fileName"));
        assertEquals("250.0", state.get("totalAmount"));
        assertEquals("APPROVED", state.get("status"));
    }
}
