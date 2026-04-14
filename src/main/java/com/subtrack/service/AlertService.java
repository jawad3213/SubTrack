package com.subtrack.service;

import com.subtrack.dao.AlertRuleDAO;
import com.subtrack.dao.NotificationLogDAO;
import com.subtrack.dao.SystemConfigDAO;
import com.subtrack.entity.AlertRule;
import com.subtrack.entity.NotificationLog;
import com.subtrack.entity.Subscription;
import com.subtrack.enums.AlertChannel;
import com.subtrack.util.AppLogger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.logging.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AlertService {

    private static final Logger LOGGER = Logger.getLogger(AlertService.class.getName());
    private static final AppLogger APP_LOGGER = AppLogger.getLogger(AlertService.class);

    @Inject
    private AlertRuleDAO alertRuleDAO;

    @Inject
    private EmailService emailService;

    @Inject
    private SystemConfigDAO configDAO;

    @Inject
    private NotificationLogDAO notificationLogDAO;

    public List<AlertRule> findAll() {
        return alertRuleDAO.findAll();
    }

    public List<AlertRule> findBySubscriptionId(UUID subscriptionId) {
        return alertRuleDAO.findBySubscriptionId(subscriptionId);
    }

    public Optional<AlertRule> findById(UUID id) {
        return alertRuleDAO.findById(id);
    }

    @Transactional
    public void createAlert(AlertRule alertRule) {
        alertRuleDAO.create(alertRule);
    }

    @Transactional
    public void updateAlert(AlertRule alertRule) {
        alertRuleDAO.update(alertRule);
    }

    @Transactional
    public void deleteAlert(AlertRule alertRule) {
        alertRuleDAO.delete(alertRule);
    }
    
    @Transactional
    public void createAlertForSubscription(Subscription subscription, com.subtrack.enums.AlertChannel channel, int timingDays) {
        AlertRule alertRule = new AlertRule();
        alertRule.setSubscription(subscription);
        alertRule.setChannel(channel);
        alertRule.setTimingDays(timingDays);
        alertRule.setIsActive(true);
        alertRuleDAO.create(alertRule);
    }
    
    public List<AlertRule> getActiveAlerts() {
        return alertRuleDAO.findActiveAlertRules();
    }
    
    public List<AlertRule> getAlertsDueSoon(int days) {
        return alertRuleDAO.findAlertsDueSoon(days);
    }
    
    public void sendEmailNotification(AlertRule alert) {
        if (alert.getSubscription() != null && alert.getSubscription().getClient() != null) {
            String clientEmail = alert.getSubscription().getClient().getEmail();
            String subName = alert.getSubscription().getName();
            
            String subject = "Subscription Alert: " + subName;
            String body = "Hello " + alert.getSubscription().getClient().getFirstName() + ",\n\n"
                    + "Your subscription for '" + subName + "' is due soon.\n\n"
                    + "Please check your SubTrack dashboard for more details.\n\n"
                    + "Thank you,\nSubTrack Team";

            try {
                emailService.sendEmail(clientEmail, subject, body);
                logNotificationEmail(alert, "SENT", null);
            } catch (Exception e) {
                logNotificationEmail(alert, "FAILED", e.getMessage());
            }
        }
    }

    private void logNotificationEmail(AlertRule alert, String status, String error) {
        try {
            NotificationLog log = new NotificationLog();
            log.setSubscription(alert.getSubscription());
            if (alert.getSubscription() != null && alert.getSubscription().getClient() != null) {
                log.setClientEmail(alert.getSubscription().getClient().getEmail());
                log.setRecipientIdentifier(alert.getSubscription().getClient().getEmail());
            }
            log.setChannel(alert.getChannel());
            log.setDeliveryStatus(status);
            log.setErrorMessage(error);
            notificationLogDAO.create(log);
        } catch (Exception e) {
            System.err.println("Failed to log email notification: " + e.getMessage());
        }
    }
    
    public void sendTelegramNotification(AlertRule alert) {
        if (alert.getSubscription() == null || alert.getSubscription().getClient() == null) {
            return;
        }

        var client = alert.getSubscription().getClient();
        String chatId = client.getTelegramChatId();
        if (chatId == null || chatId.isBlank()) {
            logNotification(alert, "FAILED", "No Telegram chat ID configured");
            return;
        }

        String botToken = configDAO.getValue("TELEGRAM_BOT_TOKEN", "");
        if (botToken.isBlank()) {
            logNotification(alert, "FAILED", "Telegram bot token not configured");
            return;
        }

        String subName = alert.getSubscription().getName();
        String message = "⏰ Subscription Alert\n\n" +
                "Your subscription for '" + subName + "' is due soon.\n\n" +
                "Please check your SubTrack dashboard for more details.\n\n" +
                "Thank you,\nSubTrack Team";

        try {
            URL url = new URL("https://api.telegram.org/bot" + botToken + "/sendMessage");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            String jsonBody = "{\"chat_id\":\"" + chatId + "\",\"text\":\"" + escapeJson(message) + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                logNotification(alert, "SENT", null);
            } else {
                String error = "HTTP " + responseCode;
                logNotification(alert, "FAILED", error);
            }
        } catch (Exception e) {
            logNotification(alert, "FAILED", e.getMessage());
        }
    }

    private void logNotification(AlertRule alert, String status, String error) {
        try {
            NotificationLog log = new NotificationLog();
            log.setSubscription(alert.getSubscription());
            if (alert.getSubscription() != null && alert.getSubscription().getClient() != null) {
                log.setClientEmail(alert.getSubscription().getClient().getEmail());
                log.setRecipientIdentifier(alert.getSubscription().getClient().getTelegramChatId());
            }
            log.setChannel(alert.getChannel());
            log.setDeliveryStatus(status);
            log.setErrorMessage(error);
            notificationLogDAO.create(log);
        } catch (Exception e) {
            LOGGER.severe("Failed to log notification: " + e.getMessage());
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }
    
    public void sendWhatsAppNotification(AlertRule alert) {
        if (alert.getSubscription() == null || alert.getSubscription().getClient() == null) {
            return;
        }

        var client = alert.getSubscription().getClient();
        String whatsappNumber = client.getWhatsappNumber();
        if (whatsappNumber == null || whatsappNumber.isBlank()) {
            logNotificationWhatsApp(alert, "FAILED", "No WhatsApp number configured");
            return;
        }

        String endpoint = configDAO.getValue("WHATSAPP_ENDPOINT", "");
        String apiKey = configDAO.getValue("WHATSAPP_API_KEY", "");
        if (endpoint.isBlank()) {
            logNotificationWhatsApp(alert, "FAILED", "WhatsApp endpoint not configured");
            return;
        }

        String subName = alert.getSubscription().getName();
        String message = "⏰ Subscription Alert\n\n" +
                "Your subscription for '" + subName + "' is due soon.\n\n" +
                "Please check your SubTrack dashboard for more details.\n\n" +
                "Thank you,\nSubTrack Team";

        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            if (apiKey != null && !apiKey.isBlank()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            String jsonBody = "{\"to\":\"" + whatsappNumber + "\",\"message\":\"" + escapeJson(message) + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                logNotificationWhatsApp(alert, "SENT", null);
            } else {
                String error = "HTTP " + responseCode;
                logNotificationWhatsApp(alert, "FAILED", error);
            }
        } catch (Exception e) {
            logNotificationWhatsApp(alert, "FAILED", e.getMessage());
        }
    }

    private void logNotificationWhatsApp(AlertRule alert, String status, String error) {
        try {
            NotificationLog log = new NotificationLog();
            log.setSubscription(alert.getSubscription());
            if (alert.getSubscription() != null && alert.getSubscription().getClient() != null) {
                log.setClientEmail(alert.getSubscription().getClient().getEmail());
                log.setRecipientIdentifier(alert.getSubscription().getClient().getWhatsappNumber());
            }
            log.setChannel(AlertChannel.WHATSAPP);
            log.setDeliveryStatus(status);
            log.setErrorMessage(error);
            notificationLogDAO.create(log);
        } catch (Exception e) {
            System.err.println("Failed to log WhatsApp notification: " + e.getMessage());
        }
    }
    
    public void checkAndSendAlerts() {
        List<AlertRule> alerts = alertRuleDAO.findAlertsDueSoon(3);
        for (AlertRule alert : alerts) {
            switch (alert.getChannel()) {
                case EMAIL -> sendEmailNotification(alert);
                case TELEGRAM -> sendTelegramNotification(alert);
                case WHATSAPP -> sendWhatsAppNotification(alert);
            }
        }
    }
}
