package com.example.invoicetracker.service.ai;

import com.example.invoicetracker.model.entity.Invoice;
import com.example.invoicetracker.repository.InvoiceRepository;
import com.example.invoicetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Locale;


@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceChatbotService {

    private final GeminiService geminiService;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;

    /**
     * Processes a natural language query about invoices.
     * 
     * @param query 
     * @param username 
     * @param role 
     * @return 
     */
    public String chat(String query, String username, String role) {
        try {
            log.info("Processing chat query: {} for user: {}", query, username);
            
            // Step 1: Get user's invoice data
            String invoiceData = collectInvoiceData(username, role);
            
            // Step 2: Build context-aware prompt
            String prompt = buildChatPrompt(query, invoiceData, username);
            
            // Step 3: Get AI response
            String response = geminiService.generateText(prompt);
            
            log.debug("Chat response: {}", response);
            return response;
            
        } catch (Exception e) {
            log.error("Error processing chat: {}", e.getMessage(), e);
            return "Sorry, I encountered an error processing your request: " + e.getMessage();
        }
    }

    /**
     * Returns raw invoice data for debugging purposes.
     * 
     * @param username 
     * @param role 
     * @return 
     */
    public String getInvoiceDataForDebug(String username, String role) {
        return collectInvoiceData(username, role);
    }

    
    
    private String collectInvoiceData(String username, String role) {
        List<Invoice> invoices;
        
        if ("SUPERUSER".equals(role) || "AUDITOR".equals(role)) {
            List<Invoice> allInvoices = invoiceRepository.findAll();
            log.info("Total invoices in DB: {}", allInvoices.size());
            
            invoices = allInvoices.stream()
                    .filter(Invoice::getIsActive)
                    .collect(Collectors.toList());
            
            log.info("Active invoices for SUPERUSER/AUDITOR {}: {}", username, invoices.size());
        } else {
            var user = userRepository.findByUsername(username).orElseThrow();
            invoices = invoiceRepository.findAllByUploadedByUserAndIsActiveTrue(
                    user, org.springframework.data.domain.PageRequest.of(0, 1000)).getContent();
            
            log.info("Active invoices for USER {}: {}", username, invoices.size());
        }
        
        log.debug("Invoice IDs: {}", invoices.stream()
                .map(Invoice::getInvoiceId)
                .collect(Collectors.toList()));
        
        return formatInvoiceDataForAI(invoices);
    }

    
    
    private String formatInvoiceDataForAI(List<Invoice> invoices) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== INVOICE DATABASE ===\n\n");
        
        // Check if empty
        if (invoices.isEmpty()) {
            sb.append("⚠️ NO INVOICES FOUND IN THE DATABASE\n");
            return sb.toString();
        }

        int totalInvoices = invoices.size();
        double totalAmount = invoices.stream()
                .mapToDouble(Invoice::getTotalAmount)
                .sum();
        
        sb.append(String.format("📊 SUMMARY:\n"));
        sb.append(String.format("   Total Invoices: %d\n", totalInvoices));
        sb.append(String.format("   Total Amount: $%.2f\n\n", totalAmount));
        
        // Monthly breakdown
        Map<String, Double> monthlyTotals = invoices.stream()
                .collect(Collectors.groupingBy(
                        inv -> inv.getInvoiceDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) 
                                + " " + inv.getInvoiceDate().getYear(),
                        Collectors.summingDouble(Invoice::getTotalAmount)
                ));
        
        Map<String, Long> monthlyCount = invoices.stream()
                .collect(Collectors.groupingBy(
                        inv -> inv.getInvoiceDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) 
                                + " " + inv.getInvoiceDate().getYear(),
                        Collectors.counting()
                ));
        
        sb.append("📅 MONTHLY BREAKDOWN:\n");
        monthlyTotals.forEach((month, amount) -> 
                sb.append(String.format("   %s: %d invoices, Total: $%.2f\n", 
                        month, monthlyCount.get(month), amount)));
        
        // File type breakdown
        Map<String, Long> fileTypeCount = invoices.stream()
                .collect(Collectors.groupingBy(
                        inv -> inv.getFileType().toString(),
                        Collectors.counting()
                ));
        
        sb.append("\n📁 FILE TYPE BREAKDOWN:\n");
        fileTypeCount.forEach((type, count) -> {
            double percentage = (count * 100.0) / totalInvoices;
            sb.append(String.format("   %s: %d invoices (%.1f%%)\n", type, count, percentage));
        });
        
        // Status breakdown
        Map<String, Long> statusCount = invoices.stream()
                .collect(Collectors.groupingBy(
                        inv -> inv.getStatus().toString(),
                        Collectors.counting()
                ));
        
        sb.append("\n✅ STATUS BREAKDOWN:\n");
        statusCount.forEach((status, count) -> {
            double percentage = (count * 100.0) / totalInvoices;
            sb.append(String.format("   %s: %d invoices (%.1f%%)\n", status, count, percentage));
        });
        
        // User breakdown (for SUPERUSER/AUDITOR)
        Map<String, Long> userCount = invoices.stream()
                .collect(Collectors.groupingBy(
                        inv -> inv.getUploadedByUser().getUsername(),
                        Collectors.counting()
                ));
        
        Map<String, Double> userTotalAmount = invoices.stream()
                .collect(Collectors.groupingBy(
                        inv -> inv.getUploadedByUser().getUsername(),
                        Collectors.summingDouble(Invoice::getTotalAmount)
                ));
        
        if (userCount.size() > 1) {
            sb.append("\n👥 INVOICES BY USER:\n");
            userCount.forEach((user, count) -> {
                double userTotal = userTotalAmount.get(user);
                sb.append(String.format("   %s: %d invoices, Total Amount: $%.2f\n", user, count, userTotal));
            });
        }
        
        sb.append("\n📋 ALL INVOICES (sorted by date, newest first):\n");
        invoices.stream()
                .sorted((a, b) -> b.getInvoiceDate().compareTo(a.getInvoiceDate()))
                .forEach(inv -> {
                    String monthYear = inv.getInvoiceDate().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) 
                            + " " + inv.getInvoiceDate().getYear();
                    sb.append(String.format(
                            "   • Invoice #%d | Date: %s (%s) | Type: %s | Amount: $%.2f | Status: %s | Uploaded by: %s\n",
                            inv.getInvoiceId(),
                            inv.getInvoiceDate(),
                            monthYear,
                            inv.getFileType(),
                            inv.getTotalAmount(),
                            inv.getStatus(),
                            inv.getUploadedByUser().getUsername()
                    ));
                });
        
        return sb.toString();
    }

    
    
    private String buildChatPrompt(String userQuery, String invoiceData, String username) {
        String currentDate = java.time.LocalDate.now().toString();
        String currentMonth = java.time.LocalDate.now().getMonth().getDisplayName(
                java.time.format.TextStyle.FULL, Locale.ENGLISH) + " " + java.time.LocalDate.now().getYear();
        java.time.LocalDate lastMonth = java.time.LocalDate.now().minusMonths(1);
        String lastMonthStr = lastMonth.getMonth().getDisplayName(
                java.time.format.TextStyle.FULL, Locale.ENGLISH) + " " + lastMonth.getYear();
        
        return String.format("""
            You are an intelligent invoice analytics assistant for user "%s".
            
            📅 CURRENT DATE CONTEXT:
            - Today's Date: %s
            - Current Month: %s
            - Last Month: %s
            
            ⚠️ CRITICAL INSTRUCTIONS:
            1. When the user asks about "this month" or "current month", they mean: %s
            2. When the user asks about "last month", they mean: %s
            3. Look at the invoice dates in the data below and match them to these months
            4. The data below shows ALL invoices with their EXACT dates and months in parentheses
            
            📊 AVAILABLE DATA FIELDS:
            The invoice data below includes:
            - SUMMARY: Total count and amount
            - MONTHLY BREAKDOWN: Invoices grouped by month with totals
            - FILE TYPE BREAKDOWN: Count and percentage of PDF vs IMAGE vs WEB_FORM invoices
            - STATUS BREAKDOWN: Count and percentage of PENDING, PAID, OVERDUE, etc.
            - INVOICES BY USER: Which user uploaded how many invoices (if multiple users exist)
            - ALL INVOICES: Complete list with ID, Date, Month, Type, Amount, Status, and Uploader
            
            %s
            
            👤 User Question: %s
            
            📋 Response Guidelines:
            - Answer based STRICTLY on the invoice data provided above
            - The data includes file types (PDF, IMAGE, WEB_FORM) - use this for file type questions
            - The data includes user information - use this for questions about specific users
            - The data includes monthly breakdowns - use this for date-range questions
            - If you see invoices with dates from %s in the data, that means there ARE invoices from this month
            - Be precise with numbers - count the invoices carefully or use the summary statistics
            - Include specific invoice IDs, dates, and amounts when relevant
            - For percentage questions, use the provided breakdown statistics
            - For user comparisons, use the "INVOICES BY USER" section
            - Format currency as $X.XX
            - Use bullet points for clarity
            - If the user asks about a period or category with no invoices, clearly state that
            
            Now provide your answer:
            """, username, currentDate, currentMonth, lastMonthStr, currentMonth, lastMonthStr, 
                invoiceData, userQuery, currentMonth);
    }
}

