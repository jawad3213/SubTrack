package com.subtrack.controller;

import com.subtrack.dao.SystemConfigDAO;
import com.subtrack.service.GeminiService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.Serializable;
import java.util.Properties;

/**
 * Bean for API Keys & Configuration admin page.
 * Values are stored in the SystemConfig table.
 */
@Named
@ApplicationScoped
public class AdminConfigBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SystemConfigDAO configDAO;

    @Inject
    private GeminiService geminiService;

    private String geminiApiKey = "";
    private String exchangeRateApiKey = "";
    private String exchangeRateUrl = "";
    private String n8nWebhookUrl = "";
    private String telegramBotToken = "";
    private String whatsappApiKey = "";
    private String whatsappEndpoint = "";
    private String smtpHost = "";
    private String smtpPort = "587";
    private String smtpUsername = "";
    private String smtpPassword = "";

    @PostConstruct
    public void init() {
        geminiApiKey = configDAO.getValue("GEMINI_API_KEY", "");
        exchangeRateApiKey = configDAO.getValue("EXCHANGE_RATE_API_KEY", "");
        exchangeRateUrl = configDAO.getValue("EXCHANGE_RATE_URL", "");
        n8nWebhookUrl = configDAO.getValue("N8N_WEBHOOK_URL", "");
        telegramBotToken = configDAO.getValue("TELEGRAM_BOT_TOKEN", "");
        whatsappApiKey = configDAO.getValue("WHATSAPP_API_KEY", "");
        whatsappEndpoint = configDAO.getValue("WHATSAPP_ENDPOINT", "");
        smtpHost = configDAO.getValue("SMTP_HOST", "");
        smtpPort = configDAO.getValue("SMTP_PORT", "587");
        smtpUsername = configDAO.getValue("SMTP_USER", "");
        smtpPassword = configDAO.getValue("SMTP_PASS", "");
    }

    public void testGemini() {
        try {
            // Save current key first so the service can read it
            configDAO.setValue("GEMINI_API_KEY", geminiApiKey);
            boolean ok = geminiService.testConnection();
            if (ok) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Gemini API connection successful!"));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Gemini API test failed. Check your API key."));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Gemini API error: " + e.getMessage()));
        }
    }
    public void testExchangeRate() {
        try {
            configDAO.setValue("EXCHANGE_RATE_URL", exchangeRateUrl);
            configDAO.setValue("EXCHANGE_RATE_API_KEY", exchangeRateApiKey);
            
            if (exchangeRateUrl == null || exchangeRateUrl.isBlank()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Exchange rate URL is not configured."));
                return;
            }
            
            URL url = new URL(exchangeRateUrl + "?api_key=" + exchangeRateApiKey);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Exchange Rate API connection successful!"));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Exchange Rate API returned HTTP " + responseCode));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Exchange Rate API error: " + e.getMessage()));
        }
    }
    
    public void testN8n() {
        try {
            configDAO.setValue("N8N_WEBHOOK_URL", n8nWebhookUrl);
            
            if (n8nWebhookUrl == null || n8nWebhookUrl.isBlank()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "n8n webhook URL is not configured."));
                return;
            }
            
            URL url = new URL(n8nWebhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            String testBody = "{\"test\": true}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(testBody.getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "n8n webhook connection successful!"));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "n8n webhook returned HTTP " + responseCode));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "n8n webhook error: " + e.getMessage()));
        }
    }
    
    public void testTelegram() {
        try {
            configDAO.setValue("TELEGRAM_BOT_TOKEN", telegramBotToken);
            
            if (telegramBotToken == null || telegramBotToken.isBlank()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Telegram bot token is not configured."));
                return;
            }
            
            URL url = new URL("https://api.telegram.org/bot" + telegramBotToken + "/getMe");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }
                if (response.toString().contains("\"ok\":true")) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Telegram bot is valid!"));
                } else {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Telegram bot response invalid."));
                }
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Telegram API returned HTTP " + responseCode));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Telegram API error: " + e.getMessage()));
        }
    }
    
    public void testWhatsapp() {
        try {
            configDAO.setValue("WHATSAPP_ENDPOINT", whatsappEndpoint);
            configDAO.setValue("WHATSAPP_API_KEY", whatsappApiKey);
            
            if (whatsappEndpoint == null || whatsappEndpoint.isBlank()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "WhatsApp endpoint is not configured."));
                return;
            }
            
            URL url = new URL(whatsappEndpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (whatsappApiKey != null && !whatsappApiKey.isBlank()) {
                conn.setRequestProperty("Authorization", "Bearer " + whatsappApiKey);
            }
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "WhatsApp API connection successful!"));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "WhatsApp API returned HTTP " + responseCode));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "WhatsApp API error: " + e.getMessage()));
        }
    }
    
    public void testSmtp() {
        try {
            configDAO.setValue("SMTP_HOST", smtpHost);
            configDAO.setValue("SMTP_PORT", smtpPort);
            configDAO.setValue("SMTP_USER", smtpUsername);
            configDAO.setValue("SMTP_PASS", smtpPassword);
            
            if (smtpHost == null || smtpHost.isBlank()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "SMTP host is not configured."));
                return;
            }
            
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            
            Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
                protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new jakarta.mail.PasswordAuthentication(smtpUsername, smtpPassword);
                }
            });
            
            session.setDebug(false);
            
            Transport transport = session.getTransport("smtp");
            int port = Integer.parseInt(smtpPort);
            transport.connect(smtpHost, port, smtpUsername, smtpPassword);
            transport.close();
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "SMTP connection successful!"));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "SMTP error: " + e.getMessage()));
        }
    }

    public void saveAll() {
        configDAO.setValue("GEMINI_API_KEY", geminiApiKey);
        configDAO.setValue("EXCHANGE_RATE_API_KEY", exchangeRateApiKey);
        configDAO.setValue("EXCHANGE_RATE_URL", exchangeRateUrl);
        configDAO.setValue("N8N_WEBHOOK_URL", n8nWebhookUrl);
        configDAO.setValue("TELEGRAM_BOT_TOKEN", telegramBotToken);
        configDAO.setValue("WHATSAPP_API_KEY", whatsappApiKey);
        configDAO.setValue("WHATSAPP_ENDPOINT", whatsappEndpoint);
        configDAO.setValue("SMTP_HOST", smtpHost);
        configDAO.setValue("SMTP_PORT", smtpPort);
        configDAO.setValue("SMTP_USER", smtpUsername);
        configDAO.setValue("SMTP_PASS", smtpPassword);
        
        jakarta.faces.context.FacesContext.getCurrentInstance().addMessage(null,
            new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_INFO, "Success", "Configuration saved successfully"));
    }

    // Getters and setters
    public String getGeminiApiKey() { return geminiApiKey; }
    public void setGeminiApiKey(String s) { this.geminiApiKey = s; }
    public String getExchangeRateApiKey() { return exchangeRateApiKey; }
    public void setExchangeRateApiKey(String s) { this.exchangeRateApiKey = s; }
    public String getExchangeRateUrl() { return exchangeRateUrl; }
    public void setExchangeRateUrl(String s) { this.exchangeRateUrl = s; }
    public String getN8nWebhookUrl() { return n8nWebhookUrl; }
    public void setN8nWebhookUrl(String s) { this.n8nWebhookUrl = s; }
    public String getTelegramBotToken() { return telegramBotToken; }
    public void setTelegramBotToken(String s) { this.telegramBotToken = s; }
    public String getWhatsappApiKey() { return whatsappApiKey; }
    public void setWhatsappApiKey(String s) { this.whatsappApiKey = s; }
    public String getWhatsappEndpoint() { return whatsappEndpoint; }
    public void setWhatsappEndpoint(String s) { this.whatsappEndpoint = s; }
    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String s) { this.smtpHost = s; }
    public String getSmtpPort() { return smtpPort; }
    public void setSmtpPort(String s) { this.smtpPort = s; }
    public String getSmtpUsername() { return smtpUsername; }
    public void setSmtpUsername(String s) { this.smtpUsername = s; }
    public String getSmtpPassword() { return smtpPassword; }
    public void setSmtpPassword(String s) { this.smtpPassword = s; }
}
