package com.subtrack.service;

import com.subtrack.dao.SystemConfigDAO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class WebhookService {

    @Inject
    private SystemConfigDAO configDAO;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public enum WebhookEvent {
        GEMINI_INVOICE_PARSED,
        GEMINI_ERROR,
        EXCHANGE_RATE_UPDATED,
        EXCHANGE_RATE_ERROR,
        SUBSCRIPTION_CREATED,
        SUBSCRIPTION_EXPIRED,
        PAYMENT_RECEIVED,
        SYSTEM_ERROR
    }

    public static class WebhookResult {
        private boolean success;
        private String message;
        private int httpStatusCode;

        public WebhookResult(boolean success, String message, int httpStatusCode) {
            this.success = success;
            this.message = message;
            this.httpStatusCode = httpStatusCode;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getHttpStatusCode() { return httpStatusCode; }
    }

    public WebhookResult sendWebhook(WebhookEvent event, Map<String, Object> data) {
        String webhookUrl = configDAO.getValue("N8N_WEBHOOK_URL", "");
        
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return new WebhookResult(false, "Webhook URL not configured", 0);
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", event.name());
            payload.put("timestamp", LocalDateTime.now().format(ISO_FORMATTER));
            payload.put("data", data);

            String jsonPayload = toJson(payload);
            
            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            String responseBody = "";
            
            if (responseCode >= 200 && responseCode < 300) {
                responseBody = readResponse(conn.getInputStream());
                return new WebhookResult(true, "Webhook sent successfully", responseCode);
            } else {
                responseBody = readResponse(conn.getErrorStream());
                return new WebhookResult(false, "HTTP " + responseCode + ": " + responseBody, responseCode);
            }

        } catch (Exception e) {
            return new WebhookResult(false, "Connection error: " + e.getMessage(), 0);
        }
    }

    public void notifyGeminiSuccess(String serviceName, String amount, String currency) {
        Map<String, Object> data = new HashMap<>();
        data.put("serviceName", serviceName);
        data.put("amount", amount);
        data.put("currency", currency);
        data.put("status", "success");
        sendWebhook(WebhookEvent.GEMINI_INVOICE_PARSED, data);
    }

    public void notifyGeminiError(String errorMessage, String rawContentPreview) {
        Map<String, Object> data = new HashMap<>();
        data.put("errorMessage", errorMessage);
        data.put("rawContentPreview", rawContentPreview != null && rawContentPreview.length() > 100 
            ? rawContentPreview.substring(0, 100) + "..." : rawContentPreview);
        data.put("status", "error");
        sendWebhook(WebhookEvent.GEMINI_ERROR, data);
    }

    public void notifyExchangeRateSuccess(int currenciesUpdated) {
        Map<String, Object> data = new HashMap<>();
        data.put("currenciesUpdated", currenciesUpdated);
        data.put("status", "success");
        data.put("source", "ExchangeRateScheduler");
        sendWebhook(WebhookEvent.EXCHANGE_RATE_UPDATED, data);
    }

    public void notifyExchangeRateError(String errorMessage) {
        Map<String, Object> data = new HashMap<>();
        data.put("errorMessage", errorMessage);
        data.put("status", "error");
        data.put("source", "ExchangeRateScheduler");
        sendWebhook(WebhookEvent.EXCHANGE_RATE_ERROR, data);
    }

    public void notifySubscriptionCreated(String subscriptionName, String clientEmail) {
        Map<String, Object> data = new HashMap<>();
        data.put("subscriptionName", subscriptionName);
        data.put("clientEmail", clientEmail);
        data.put("action", "created");
        sendWebhook(WebhookEvent.SUBSCRIPTION_CREATED, data);
    }

    public void notifyPaymentReceived(String subscriptionName, String amount, String currency) {
        Map<String, Object> data = new HashMap<>();
        data.put("subscriptionName", subscriptionName);
        data.put("amount", amount);
        data.put("currency", currency);
        data.put("action", "payment_received");
        sendWebhook(WebhookEvent.PAYMENT_RECEIVED, data);
    }

    private String readResponse(java.io.InputStream inputStream) {
        if (inputStream == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escapeJson(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value instanceof Map) {
                sb.append(toJson((Map<String, Object>) value));
            } else {
                sb.append("\"").append(escapeJson(String.valueOf(value))).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}