package com.subtrack.service;

import com.subtrack.dao.SystemConfigDAO;
import com.subtrack.util.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service to interact with the Google Gemini API for AI-powered features.
 * Uses java.net.HttpURLConnection to avoid extra dependencies.
 */
@ApplicationScoped
public class GeminiService {

    private static final Logger LOGGER = Logger.getLogger(GeminiService.class.getName());
    private static final AppLogger APP_LOGGER = AppLogger.getLogger(GeminiService.class);
    private static final String GEMINI_API_URL = 
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    @Inject
    private SystemConfigDAO configDAO;

    @Inject
    private WebhookService webhookService;

    /**
     * Result object holding extracted invoice data from AI parsing.
     */
    public static class InvoiceParseResult {
        private String serviceName;
        private BigDecimal amount;
        private String currency;
        private LocalDate invoiceDate;
        private boolean success;
        private String errorMessage;

        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public LocalDate getInvoiceDate() { return invoiceDate; }
        public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    /**
     * Sends a prompt to the Gemini API and returns the text response.
     */
    public String sendPrompt(String prompt) {
        String apiKey = configDAO.getValue("GEMINI_API_KEY", "");
        if (apiKey.isEmpty()) {
            LOGGER.warning("GeminiService: GEMINI_API_KEY is not configured.");
            APP_LOGGER.error("GEMINI_API_KEY not configured");
            return null;
        }

        try {
            URL url = new URL(GEMINI_API_URL + "?key=" + apiKey);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            String escapedPrompt = escapeJson(prompt);
            String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapedPrompt + "\"}]}]}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
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
                LOGGER.info("Gemini API call successful");
                return extractTextFromResponse(response.toString());
            } else {
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                }
                
                String errorStr = errorResponse.toString();
                String extractedMessage = extractJsonStringValue(errorStr, "\"message\"");
                String extractedStatus = extractJsonStringValue(errorStr, "\"status\"");
                
                String finalErrorMsg = (extractedStatus != null ? "[" + extractedStatus + "] " : "") + 
                                       (extractedMessage != null ? extractedMessage : "HTTP " + responseCode);
                
                LOGGER.log(Level.WARNING, "Gemini API returned HTTP " + responseCode + ": " + errorStr);
                APP_LOGGER.logError("Gemini API HTTP error: " + finalErrorMsg);
                
                webhookService.notifyGeminiError(finalErrorMsg, prompt.substring(0, Math.min(100, prompt.length())));
                throw new RuntimeException(finalErrorMsg);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "GeminiService: Failed to call Gemini API", e);
            APP_LOGGER.logError("Gemini connection failed: " + e.getMessage());
            
            webhookService.notifyGeminiError("Connection failed: " + e.getMessage(), null);
            throw new RuntimeException("Connection failed: " + e.getMessage());
        }
    }

    /**
     * Uses the Gemini API to extract invoice data from raw email/receipt text.
     */
    public InvoiceParseResult extractInvoiceData(String rawContent) {
        InvoiceParseResult result = new InvoiceParseResult();

        if (rawContent == null || rawContent.isBlank()) {
            result.setSuccess(false);
            result.setErrorMessage("Raw content is empty.");
            LOGGER.warning("GeminiService: Raw content is empty");
            return result;
        }

        String prompt = "You are an invoice parser. Analyze the following email or receipt text and extract the following information. " +
                "Respond ONLY with a single line in this exact format (no extra text, no markdown, no explanation):\n" +
                "SERVICE_NAME|AMOUNT|CURRENCY|DATE\n\n" +
                "Rules:\n" +
                "- SERVICE_NAME: The name of the service or product (e.g., Netflix, Spotify, Adobe Creative Cloud)\n" +
                "- AMOUNT: The numeric amount charged (e.g., 15.99). Use only digits and a decimal point.\n" +
                "- CURRENCY: The 3-letter ISO currency code (e.g., USD, EUR, MAD)\n" +
                "- DATE: The invoice or charge date in yyyy-MM-dd format (e.g., 2024-03-15)\n" +
                "- If you cannot determine a field, use UNKNOWN\n\n" +
                "Here is the text to parse:\n\n" + rawContent;

        String response;
        try {
            response = sendPrompt(prompt);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("Gemini API call failed: " + e.getMessage());
            LOGGER.log(Level.WARNING, "GeminiService: API call failed during invoice parsing", e);
            return result;
        }

        if (response == null || response.isBlank()) {
            result.setSuccess(false);
            result.setErrorMessage("No response from Gemini API.");
            LOGGER.warning("GeminiService: No response from Gemini API");
            return result;
        }

        try {
            String cleanResponse = response.trim();
            String[] parts = cleanResponse.split("\\|");

            if (parts.length >= 4) {
                String serviceName = parts[0].trim();
                if (!"UNKNOWN".equalsIgnoreCase(serviceName)) {
                    result.setServiceName(serviceName);
                }

                String amountStr = parts[1].trim();
                if (!"UNKNOWN".equalsIgnoreCase(amountStr)) {
                    try {
                        result.setAmount(new BigDecimal(amountStr));
                    } catch (NumberFormatException e) {
                        LOGGER.warning("GeminiService: Could not parse amount: " + amountStr);
                    }
                }

                String currency = parts[2].trim();
                if (!"UNKNOWN".equalsIgnoreCase(currency) && currency.length() == 3) {
                    result.setCurrency(currency);
                }

                String dateStr = parts[3].trim();
                if (!"UNKNOWN".equalsIgnoreCase(dateStr)) {
                    try {
                        result.setInvoiceDate(LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE));
                    } catch (DateTimeParseException e) {
                        LOGGER.warning("GeminiService: Could not parse date: " + dateStr);
                    }
                }

                result.setSuccess(true);
                LOGGER.info("GeminiService: Successfully parsed invoice - " + 
                    (result.getServiceName() != null ? result.getServiceName() : "unknown"));
                
                webhookService.notifyGeminiSuccess(
                    result.getServiceName() != null ? result.getServiceName() : "UNKNOWN",
                    result.getAmount() != null ? result.getAmount().toString() : "UNKNOWN",
                    result.getCurrency() != null ? result.getCurrency() : "UNKNOWN"
                );
            } else {
                result.setSuccess(false);
                result.setErrorMessage("Unexpected AI response format: " + cleanResponse);
                LOGGER.warning("GeminiService: Unexpected AI response format: " + cleanResponse);
                
                webhookService.notifyGeminiError("Unexpected response format: " + cleanResponse, rawContent);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("Error parsing AI response: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "GeminiService: Error parsing AI response", e);
            
            webhookService.notifyGeminiError("Error parsing AI response: " + e.getMessage(), rawContent);
        }

        return result;
    }

    /**
     * Sends a simple test prompt to verify the API key works.
     * Returns true if successful, false otherwise.
     */
    public boolean testConnection() {
        String response = sendPrompt("Respond with exactly: OK");
        return response != null && !response.isBlank();
    }

    /**
     * Extracts the text content from the Gemini API JSON response.
     * The response format is:
     * {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}
     */
    private String extractTextFromResponse(String json) {
        // Simple JSON parsing without external libraries
        // Looking for: "text":"<content>"
        int textIndex = json.indexOf("\"text\"");
        if (textIndex == -1) {
            return null;
        }
        // Find the colon after "text"
        int colonIndex = json.indexOf(":", textIndex);
        if (colonIndex == -1) {
            return null;
        }
        // Find the opening quote of the value
        int startQuote = json.indexOf("\"", colonIndex + 1);
        if (startQuote == -1) {
            return null;
        }
        // Find the closing quote (handling escaped quotes)
        int endQuote = findClosingQuote(json, startQuote + 1);
        if (endQuote == -1) {
            return null;
        }
        String extracted = json.substring(startQuote + 1, endQuote);
        // Unescape JSON string
        return extracted.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private int findClosingQuote(String json, int startPos) {
        for (int i = startPos; i < json.length(); i++) {
            if (json.charAt(i) == '"' && json.charAt(i - 1) != '\\') {
                return i;
            }
        }
        return -1;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String extractJsonStringValue(String json, String keyWithQuotes) {
        int keyIndex = json.indexOf(keyWithQuotes);
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;
        
        int startQuote = json.indexOf("\"", colonIndex);
        if (startQuote == -1) return null;
        
        int endQuote = findClosingQuote(json, startQuote + 1);
        if (endQuote == -1) return null;
        
        return json.substring(startQuote + 1, endQuote).replace("\\n", " ").replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
