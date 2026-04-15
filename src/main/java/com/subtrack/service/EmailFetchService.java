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
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;

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
        List<String> ids = new java.util.ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject obj = reader.readObject();
            if (obj.containsKey("messages")) {
                JsonArray messages = obj.getJsonArray("messages");
                for (int i = 0; i < messages.size(); i++) {
                    ids.add(messages.getJsonObject(i).getString("id"));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse Gmail message IDs: " + e.getMessage());
        }
        return ids;
    }
    
    private String extractEmailBody(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject obj = reader.readObject();
            
            String snippet = obj.getString("snippet", null);
            if (snippet != null && !snippet.isBlank()) {
                return snippet;
            }
            
            if (obj.containsKey("payload")) {
                JsonObject payload = obj.getJsonObject("payload");
                if (payload.containsKey("body")) {
                    JsonObject body = payload.getJsonObject("body");
                    if (body.containsKey("data")) {
                        String encoded = body.getString("data");
                        // Replace URL-safe base64 characters
                        String base64 = encoded.replace("-", "+").replace("_", "/");
                        return new String(java.util.Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to extract email body: " + e.getMessage());
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