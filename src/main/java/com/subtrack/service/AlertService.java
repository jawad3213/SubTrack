package com.subtrack.service;

import com.subtrack.dao.AlertRuleDAO;
import com.subtrack.entity.AlertRule;
import com.subtrack.entity.Subscription;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AlertService {

    @Inject
    private AlertRuleDAO alertRuleDAO;

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
        
    }
    
    public void sendTelegramNotification(AlertRule alert) {
        
    }
    
    public void sendWhatsAppNotification(AlertRule alert) {
        
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
