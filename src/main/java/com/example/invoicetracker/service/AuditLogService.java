package com.example.invoicetracker.service;

import com.example.invoicetracker.model.entity.Invoice;
import com.example.invoicetracker.model.entity.Log;
import com.example.invoicetracker.model.entity.User;
import com.example.invoicetracker.model.enums.ActionType;
import com.example.invoicetracker.model.enums.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditLogService {



    public String generateUserFriendlyDescription(Log log) {
        String user = log.getPerformedBy().getUsername();
        String action = getActionDescription(log.getActionType());
        
        if (log.getActionType() == ActionType.UPDATE && log.getOldValues() != null) {
            return generateUpdateDescription(log, user, action);
        }
        
        return String.format("%s %s", user, action);
    }

    private String getActionDescription(ActionType actionType) {
        return switch (actionType) {
            case CREATE -> "created this invoice";
            case UPDATE -> "updated this invoice";
            case DELETE -> "deleted this invoice";
            case GET -> "viewed this invoice";
            case VIEW -> "viewed this invoice details";
            case UPLOAD -> "uploaded a file for this invoice";
            case DOWNLOAD -> "downloaded the invoice file";
            default -> "performed an action on this invoice";
        };
    }

    private String generateUpdateDescription(Log log, String user, String action) {
        Map<String, Object> oldValues = log.getOldValues();
        Map<String, Object> newValues = log.getNewValues();
        
        List<String> changes = new ArrayList<>();
        
        if (oldValues.containsKey("status") && newValues.containsKey("status")) {
            String oldStatus = (String) oldValues.get("status");
            String newStatus = (String) newValues.get("status");
            changes.add(String.format("status from %s to %s", oldStatus, newStatus));
        }
        
        if (oldValues.containsKey("totalAmount") && newValues.containsKey("totalAmount")) {
            String oldAmount = (String) oldValues.get("totalAmount");
            String newAmount = (String) newValues.get("totalAmount");
            changes.add(String.format("total amount from $%s to $%s", oldAmount, newAmount));
        }
        
        if (oldValues.containsKey("fileName") && newValues.containsKey("fileName")) {
            String oldName = (String) oldValues.get("fileName");
            String newName = (String) newValues.get("fileName");
            changes.add(String.format("file name from '%s' to '%s'", oldName, newName));
        }
        
        if (changes.isEmpty()) {
            return String.format("%s %s", user, action);
        }
        
        return String.format("%s %s: %s", user, action, String.join(", ", changes));
    }

    public void logInvoiceAction(Invoice invoice, User user, ActionType actionType, 
                               Map<String, Object> oldValues, Map<String, Object> newValues) {
        Log logEntry = Log.builder()
                .invoice(invoice)
                .performedBy(user)
                .actionType(actionType)
                .timestamp(LocalDateTime.now())
                .oldValues(oldValues)
                .newValues(newValues)
                .build();

        addLogToInvoice(invoice, logEntry);
    }

    public void logStatusChange(Invoice invoice, User user, InvoiceStatus oldStatus, InvoiceStatus newStatus) {
        Map<String, Object> oldValues = Map.of("status", oldStatus.name());
        Map<String, Object> newValues = Map.of("status", newStatus.name());
        logInvoiceAction(invoice, user, ActionType.UPDATE, oldValues, newValues);
    }

    public Map<String, Object> captureInvoiceState(Invoice invoice) {
        return Map.of(
                "invoiceDate", invoice.getInvoiceDate().toString(),
                "fileType", invoice.getFileType().name(),
                "fileName", invoice.getFileName(),
                "totalAmount", invoice.getTotalAmount().toString(),
                "status", invoice.getStatus().name()
        );
    }

    private void addLogToInvoice(Invoice invoice, Log logEntry) {
        if (invoice.getLog() == null) {
            invoice.setLog(new ArrayList<>(List.of(logEntry)));
        } else {
            invoice.getLog().add(logEntry);
        }
    }
}