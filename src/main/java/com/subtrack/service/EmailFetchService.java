package com.subtrack.service;

import com.subtrack.dao.SystemConfigDAO;
import com.subtrack.entity.Client;
import com.subtrack.entity.EmailIntegration;
import com.subtrack.entity.Invoice;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EmailFetchService {

    @Inject
    private EmailIntegrationService emailIntegrationService;
    
    @Inject
    private InvoiceService invoiceService;
    
    @Inject
    private SystemConfigDAO systemConfigDAO;
    
    private static final String GMAIL_API = "https://gmail.googleapis.com/gmail/v1/users/me/messages";
    private static final String GMAIL_GET_API = "https://gmail.googleapis.com/gmail/v1/users/me/messages/";
    
    @Transactional
    public void fetchEmailsForClient(UUID clientId) {
        Optional<EmailIntegration> integrationOpt = emailIntegrationService.findByClientId(clientId);
        if (integrationOpt.isEmpty()) {
            return;
        }
        
        EmailIntegration integration = integrationOpt.get();
        if (!integration.getIsActive() || integration.isTokenExpired()) {
            return;
        }
        
        String accessToken = integration.getAccessToken();
        try {
            List<String> unreadIds = fetchUnreadEmailIds(accessToken);
            for (String emailId : unreadIds) {
                String emailContent = fetchEmailContent(accessToken, emailId);
                if (emailContent != null && looksLikeInvoice(emailContent)) {
                    createInvoiceRecord(integration.getClient(), emailContent, emailId);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch emails: " + e.getMessage());
        }
    }
    
    private List<String> fetchUnreadEmailIds(String accessToken) throws Exception {
        URL url = new URL(GMAIL_API + "?q=is:unread subject:invoice subject:receipt subject:billing&maxResults=20");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            if (responseCode == 401) {
                System.err.println("Gmail API: Token expired");
            }
            return java.util.Collections.emptyList();
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        
        return parseEmailIdsFromResponse(response.toString());
    }
    
    private String fetchEmailContent(String accessToken, String emailId) throws Exception {
        URL url = new URL(GMAIL_GET_API + emailId + "?format=full");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        
        if (conn.getResponseCode() != 200) {
            return null;
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        
        return extractEmailBody(response.toString());
    }
    
    private List<String> parseEmailIdsFromResponse(String json) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        int idx = 0;
        while ((idx = json.indexOf("\"id\"", idx)) != -1) {
            int colon = json.indexOf(":", idx);
            int quote1 = json.indexOf("\"", colon);
            int quote2 = json.indexOf("\"", quote1 + 1);
            if (colon > 0 && quote1 > 0 && quote2 > 0) {
                ids.add(json.substring(quote1 + 1, quote2));
            }
            idx = quote2;
        }
        return ids;
    }
    
    private String extractEmailBody(String json) {
        String snippet = extractJsonField(json, "snippet");
        if (snippet != null) {
            return snippet;
        }
        
        int payloadIdx = json.indexOf("\"payload\"");
        if (payloadIdx == -1) return null;
        
        int bodyIdx = json.indexOf("\"body\"", payloadIdx);
        if (bodyIdx == -1) return null;
        
        int dataIdx = json.indexOf("\"data\"", bodyIdx);
        if (dataIdx == -1) return null;
        
        int colon = json.indexOf(":", dataIdx);
        int quote1 = json.indexOf("\"", colon);
        int quote2 = json.indexOf("\"", quote1 + 1);
        if (quote1 > 0 && quote2 > 0) {
            String encoded = json.substring(quote1 + 1, quote2);
            return new String(java.util.Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        }
        
        return null;
    }
    
    private String extractJsonField(String json, String field) {
        int idx = json.indexOf("\"" + field + "\"");
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx);
        if (colon == -1) return null;
        int start = json.indexOf("\"", colon);
        if (start == -1) return null;
        int end = json.indexOf("\"", start + 1);
        if (end == -1) return null;
        return json.substring(start + 1, end);
    }
    
    private boolean looksLikeInvoice(String content) {
        if (content == null) return false;
        String lower = content.toLowerCase();
        return lower.contains("invoice") || lower.contains("receipt") 
            || lower.contains("billing") || lower.contains("subscription")
            || lower.contains("amount") || lower.contains("charged");
    }
    
    private void createInvoiceRecord(Client client, String content, String emailId) {
        invoiceService.createInvoice(client, content, null, null, null, null);
    }
}