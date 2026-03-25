package com.subtrack.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Stub bean for Alert & Notification Logs admin page.
 */
@Named
@RequestScoped
public class AdminAlertLogBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String filterChannel;
    private String filterDeliveryStatus;
    private List<Object> alertLogs = new ArrayList<>();

    public void applyFilters() {
        // Stub
    }

    // Getters and setters
    public String getFilterChannel() { return filterChannel; }
    public void setFilterChannel(String s) { this.filterChannel = s; }
    public String getFilterDeliveryStatus() { return filterDeliveryStatus; }
    public void setFilterDeliveryStatus(String s) { this.filterDeliveryStatus = s; }
    public List<Object> getAlertLogs() { return alertLogs; }
}
