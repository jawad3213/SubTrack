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
    
    public int fetchEmailsForClient(UUID clientId) {
        System.out.println(">>> EmailFetchService: Starting sync for client " + clientId);
        int totalProcessed = 0;
        
        Optional<EmailIntegration> integrationOpt = emailIntegrationService.findByClientId(clientId);
        if (integrationOpt.isEmpty()) {
            System.err.println(">>> EmailFetchService: No email integration found for client " + clientId);
            return 0;
        }
        
        EmailIntegration integration = integrationOpt.get();
        if (!integration.getIsActive()) {
            System.err.println(">>> EmailFetchService: Email integration is not active");
            return 0;
        }
        
        String accessToken = integration.getAccessToken();
        
        // If token is expired, try to refresh it first
        if (integration.isTokenExpired()) {
            System.out.println(">>> EmailFetchService: Token expired, attempting refresh...");
            accessToken = refreshAccessToken(integration);
            if (accessToken == null) {
                System.err.println(">>> EmailFetchService: Access token is null and refresh failed");
                return 0;
            }
        }
        
        Client client = integration.getClient();
        
        try {
            List<String> emailIds = fetchInvoiceEmailIds(accessToken);
            System.out.println(">>> EmailFetchService: Found " + emailIds.size() + " potential invoice emails in the last 30 days");
            
            if (emailIds.isEmpty()) {
                System.out.println(">>> EmailFetchService: No matching emails found.");
                return 0;
            }
            
            System.out.println(">>> EmailFetchService: Syncing for " + client.getEmail());

            for (String emailId : emailIds) {
                try {
                    System.out.println(">>> EmailFetchService: Fetching email ID: " + emailId);
                    String emailContent = fetchEmailContent(accessToken, emailId);
                    
                    if (emailContent == null) {
                        System.err.println(">>> EmailFetchService: Could not extract content from email " + emailId);
                        continue;
                    }
                    
                    if (looksLikeInvoice(emailContent)) {
                        System.out.println(">>> EmailFetchService: Email looks like an invoice, creating record...");
                        createInvoiceRecord(client, emailContent, emailId);
                        totalProcessed++;
                        
                        // Small delay to respect Gemini 1.5-flash free-tier (15 RPM)
                        try { Thread.sleep(3500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    } else {
                        System.out.println(">>> EmailFetchService: Email " + emailId + " does NOT look like an invoice, skipping.");
                    }
                } catch (Exception e) {
                    System.err.println(">>> EmailFetchService: Error processing email " + emailId + ": " + e.getMessage());
                }
            }
            System.out.println(">>> EmailFetchService: Sync complete. Imported " + totalProcessed + " invoices.");
            return totalProcessed;
        } catch (Exception e) {
            System.err.println(">>> EmailFetchService: Failed to fetch emails: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Refresh the access token using the stored refresh token.
     */
    private String refreshAccessToken(EmailIntegration integration) {
        String refreshToken = integration.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            System.err.println(">>> EmailFetchService: No refresh token available");
            return null;
        }
        
        try {
            String clientId = systemConfigDAO.getValue("GOOGLE_OAUTH_CLIENT_ID", "");
            String clientSecret = systemConfigDAO.getValue("GOOGLE_OAUTH_CLIENT_SECRET", "");
            
            URL url = new URL("https://oauth2.googleapis.com/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            
            String params = "client_id=" + clientId
                + "&client_secret=" + clientSecret
                + "&refresh_token=" + refreshToken
                + "&grant_type=refresh_token";
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(params.getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }
                
                try (JsonReader reader = Json.createReader(new StringReader(response.toString()))) {
                    JsonObject jsonObject = reader.readObject();
                    String newAccessToken = jsonObject.getString("access_token", null);
                    int expiresIn = jsonObject.getInt("expires_in", 3600);
                    
                    if (newAccessToken != null) {
                        // Update the stored token
                        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresIn);
                        emailIntegrationService.refreshToken(integration.getClient().getId(), newAccessToken, expiresAt);
                        System.out.println(">>> EmailFetchService: Token refreshed successfully");
                        return newAccessToken;
                    }
                }
            } else {
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                }
                System.err.println(">>> EmailFetchService: Token refresh failed with HTTP " + responseCode + ": " + errorResponse);
            }
        } catch (Exception e) {
            System.err.println(">>> EmailFetchService: Token refresh error: " + e.getMessage());
        }
        return null;
    }
    
    private List<String> fetchInvoiceEmailIds(String accessToken) throws Exception {
        // Use OR logic: {subject:invoice subject:receipt subject:billing} 
        // We look for emails in the last 30 days, read or unread.
        String query = "newer_than:30d {subject:invoice subject:receipt subject:billing subject:subscription subject:payment subject:order subject:confirmation}";
        String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
        
        URL url = new URL(GMAIL_API + "?q=" + encodedQuery + "&maxResults=20");
        System.out.println(">>> EmailFetchService: Gmail API URL: " + url);
        
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        
        int responseCode = conn.getResponseCode();
        System.out.println(">>> EmailFetchService: Gmail API response code: " + responseCode);
        
        if (responseCode != 200) {
            StringBuilder errorResponse = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    errorResponse.append(line);
                }
            }
            System.err.println(">>> EmailFetchService: Gmail API error: " + errorResponse);
            
            if (responseCode == 401) {
                System.err.println(">>> EmailFetchService: Token expired or invalid (401)");
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
        
        System.out.println(">>> EmailFetchService: Gmail API response: " + response.toString().substring(0, Math.min(500, response.length())));
        return parseEmailIdsFromResponse(response.toString());
    }
    
    private String fetchEmailContent(String accessToken, String emailId) throws Exception {
        URL url = new URL(GMAIL_GET_API + emailId + "?format=full");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        
        if (conn.getResponseCode() != 200) {
            System.err.println(">>> EmailFetchService: Failed to fetch email " + emailId + " - HTTP " + conn.getResponseCode());
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
            } else {
                System.out.println(">>> EmailFetchService: No 'messages' key in Gmail response. resultSizeEstimate=" + obj.getInt("resultSizeEstimate", 0));
            }
        } catch (Exception e) {
            System.err.println(">>> EmailFetchService: Failed to parse Gmail message IDs: " + e.getMessage());
        }
        return ids;
    }
    
    private String extractEmailBody(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject obj = reader.readObject();
            
            // First try to get the subject from headers for logging
            if (obj.containsKey("payload")) {
                JsonObject payload = obj.getJsonObject("payload");
                if (payload.containsKey("headers")) {
                    JsonArray headers = payload.getJsonArray("headers");
                    for (int i = 0; i < headers.size(); i++) {
                        JsonObject header = headers.getJsonObject(i);
                        if ("Subject".equalsIgnoreCase(header.getString("name", ""))) {
                            System.out.println(">>> EmailFetchService: Email subject: " + header.getString("value", ""));
                        }
                    }
                }
                
                // Try to get body data from the payload
                if (payload.containsKey("body")) {
                    JsonObject body = payload.getJsonObject("body");
                    if (body.containsKey("data")) {
                        String encoded = body.getString("data");
                        String base64 = encoded.replace("-", "+").replace("_", "/");
                        return new String(java.util.Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
                    }
                }
                
                // Try to get body from parts (multipart emails)
                if (payload.containsKey("parts")) {
                    JsonArray parts = payload.getJsonArray("parts");
                    for (int i = 0; i < parts.size(); i++) {
                        JsonObject part = parts.getJsonObject(i);
                        String mimeType = part.getString("mimeType", "");
                        if ("text/plain".equals(mimeType) || "text/html".equals(mimeType)) {
                            if (part.containsKey("body")) {
                                JsonObject body = part.getJsonObject("body");
                                if (body.containsKey("data")) {
                                    String encoded = body.getString("data");
                                    String base64 = encoded.replace("-", "+").replace("_", "/");
                                    return new String(java.util.Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
                                }
                            }
                        }
                    }
                }
            }
            
            // Fallback: use snippet
            String snippet = obj.getString("snippet", null);
            if (snippet != null && !snippet.isBlank()) {
                System.out.println(">>> EmailFetchService: Using snippet as email body");
                return snippet;
            }
        } catch (Exception e) {
            System.err.println(">>> EmailFetchService: Failed to extract email body: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    private boolean looksLikeInvoice(String content) {
        if (content == null) return false;
        String lower = content.toLowerCase();
        return lower.contains("invoice") || lower.contains("receipt") 
            || lower.contains("billing") || lower.contains("subscription")
            || lower.contains("amount") || lower.contains("charged")
            || lower.contains("payment") || lower.contains("order")
            || lower.contains("total") || lower.contains("paid");
    }
    
    private void createInvoiceRecord(Client client, String content, String emailId) {
        // Always save the raw invoice record first
        Invoice invoice = invoiceService.createInvoice(client, content, null, null, null, null);
        if (invoice != null) {
            System.out.println(">>> EmailFetchService: Invoice record created with ID: " + invoice.getId());
            try {
                // Then try to parse it with Gemini AI
                invoiceService.parseInvoiceEmail(invoice);
                System.out.println(">>> EmailFetchService: AI parsing completed for invoice: " + invoice.getId());
            } catch (Exception e) {
                // If Gemini fails (rate limit, network, etc.) — the raw record is still saved
                System.err.println(">>> EmailFetchService: AI parsing failed for invoice " + invoice.getId() + ": " + e.getMessage() + ". Raw record saved.");
            }
        }
    }
}