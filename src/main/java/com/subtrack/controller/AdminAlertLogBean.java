package com.subtrack.controller;

import com.subtrack.dao.NotificationLogDAO;
import com.subtrack.entity.NotificationLog;
import com.subtrack.enums.AlertChannel;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@RequestScoped
public class AdminAlertLogBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private NotificationLogDAO notificationLogDAO;

    private String filterChannel;
    private String filterDeliveryStatus;
    private List<NotificationLog> alertLogs = new ArrayList<>();

    @PostConstruct
    public void init() {
        applyFilters();
    }

    public void applyFilters() {
        alertLogs.clear();
        if ((filterChannel == null || filterChannel.isBlank()) && 
            (filterDeliveryStatus == null || filterDeliveryStatus.isBlank())) {
            alertLogs = notificationLogDAO.findAll();
        } else if (filterChannel != null && !filterChannel.isBlank() && 
                   filterDeliveryStatus != null && !filterDeliveryStatus.isBlank()) {
            try {
                AlertChannel channel = AlertChannel.valueOf(filterChannel);
                alertLogs = notificationLogDAO.findByChannelAndStatus(channel, filterDeliveryStatus);
            } catch (IllegalArgumentException e) {
                alertLogs = notificationLogDAO.findAll();
            }
        } else if (filterChannel != null && !filterChannel.isBlank()) {
            try {
                AlertChannel channel = AlertChannel.valueOf(filterChannel);
                alertLogs = notificationLogDAO.findByChannel(channel);
            } catch (IllegalArgumentException e) {
                alertLogs = notificationLogDAO.findAll();
            }
        } else if (filterDeliveryStatus != null && !filterDeliveryStatus.isBlank()) {
            alertLogs = notificationLogDAO.findByDeliveryStatus(filterDeliveryStatus);
        }
    }

    public String getFilterChannel() { return filterChannel; }
    public void setFilterChannel(String s) { this.filterChannel = s; }
    public String getFilterDeliveryStatus() { return filterDeliveryStatus; }
    public void setFilterDeliveryStatus(String s) { this.filterDeliveryStatus = s; }
    public List<NotificationLog> getAlertLogs() { return alertLogs; }
}
