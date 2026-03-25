package com.subtrack.controller;

import com.subtrack.entity.AlertRule;
import com.subtrack.entity.Subscription;
import com.subtrack.enums.AlertChannel;
import com.subtrack.service.AlertService;
import com.subtrack.service.SubscriptionService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Named
@SessionScoped
public class AlertController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private AlertService alertService;
    
    @Inject
    private SubscriptionService subscriptionService;
    
    @Inject
    private UserContext userContext;

    private List<AlertRule> alertRules;
    private AlertRule selectedAlert;
    private Subscription selectedSubscription;
    private AlertChannel channel;
    private Integer timingDays = 3;

    @PostConstruct
    public void init() {
        loadAlerts();
    }
    
    public void loadAlerts() {
        if (userContext.getClientId() != null) {
            List<Subscription> subscriptions = subscriptionService.findByClientId(userContext.getClientId());
            alertRules = new java.util.ArrayList<>();
            for (Subscription sub : subscriptions) {
                alertRules.addAll(alertService.findBySubscriptionId(sub.getId()));
            }
        }
    }
    
    public String createAlert() {
        try {
            if (selectedSubscription == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please select a subscription"));
                return null;
            }
            
            alertService.createAlertForSubscription(selectedSubscription, 
                channel != null ? channel : AlertChannel.EMAIL, 
                timingDays != null ? timingDays : 3);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Alert created successfully"));
            
            loadAlerts();
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }
    
    public String deleteAlert() {
        try {
            if (selectedAlert != null) {
                alertService.deleteAlert(selectedAlert);
                
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Alert deleted"));
                
                loadAlerts();
            }
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }
    
    public String toggleAlert(AlertRule alert) {
        try {
            Boolean current = alert.getIsActive();
            alert.setIsActive(current == null || !current);
            alertService.updateAlert(alert);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Alert updated"));
            
            loadAlerts();
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }

    public List<AlertRule> getAlertRules() {
        return alertRules;
    }

    public void setAlertRules(List<AlertRule> alertRules) {
        this.alertRules = alertRules;
    }

    public AlertRule getSelectedAlert() {
        return selectedAlert;
    }

    public void setSelectedAlert(AlertRule selectedAlert) {
        this.selectedAlert = selectedAlert;
    }

    public Subscription getSelectedSubscription() {
        return selectedSubscription;
    }

    public void setSelectedSubscription(Subscription selectedSubscription) {
        this.selectedSubscription = selectedSubscription;
    }

    public AlertChannel getChannel() {
        return channel;
    }

    public void setChannel(AlertChannel channel) {
        this.channel = channel;
    }

    public Integer getTimingDays() {
        return timingDays;
    }

    public void setTimingDays(Integer timingDays) {
        this.timingDays = timingDays;
    }
    
    public AlertChannel[] getChannels() {
        return AlertChannel.values();
    }
}
