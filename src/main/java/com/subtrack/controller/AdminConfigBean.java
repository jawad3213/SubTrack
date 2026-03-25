package com.subtrack.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.io.Serializable;

/**
 * Stub bean for API Keys & Configuration admin page.
 * Values will be stored in a SystemConfig table with key-value pairs.
 */
@Named
@ApplicationScoped
public class AdminConfigBean implements Serializable {

    private static final long serialVersionUID = 1L;

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

    public void testGemini() { /* Stub */ }
    public void testExchangeRate() { /* Stub */ }
    public void testN8n() { /* Stub */ }
    public void testTelegram() { /* Stub */ }
    public void testWhatsapp() { /* Stub */ }
    public void testSmtp() { /* Stub */ }
    public void saveAll() { /* Stub: persist to SystemConfig table */ }

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
