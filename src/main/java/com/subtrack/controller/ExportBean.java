package com.subtrack.controller;

import com.subtrack.entity.Client;
import com.subtrack.entity.PaymentHistory;
import com.subtrack.entity.Subscription;
import com.subtrack.enums.SubscriptionStatus;
import com.subtrack.service.SubscriptionService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Named
@ViewScoped
public class ExportBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private UserContext userContext;

    private String subFormat = "csv";
    private String subStatus = "all";
    private String payFormat = "csv";
    private String dateRange = "all";
    private String backupFormat = "json";

    @PostConstruct
    public void init() {
    }

    public void exportSubscriptionsCsv() {
        try {
            Client user = userContext.getCurrentUser();
            if (user == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "User not logged in"));
                return;
            }

            List<Subscription> subscriptions = subscriptionService.findByClientId(user.getId());
            subscriptions = filterSubscriptions(subscriptions);

            StringBuilder csv = new StringBuilder();
            csv.append("Name,Price,Currency,Frequency,Status,Start Date,Next Billing,Category,Monthly Cost,Annual Cost\n");

            for (Subscription sub : subscriptions) {
                csv.append(escapeCsv(sub.getName())).append(",");
                csv.append(sub.getPrice()).append(",");
                csv.append(sub.getOriginalCurrency()).append(",");
                csv.append(sub.getFrequency() != null ? sub.getFrequency().name() : "").append(",");
                csv.append(sub.getStatus() != null ? sub.getStatus().name() : "").append(",");
                csv.append(sub.getStartDate() != null ? sub.getStartDate().toString() : "").append(",");
                csv.append(sub.getNextBillingDate() != null ? sub.getNextBillingDate().toString() : "").append(",");
                csv.append(sub.getCategory() != null ? escapeCsv(sub.getCategory().getName()) : "").append(",");
                csv.append(sub.getMonthlyCost()).append(",");
                csv.append(sub.getAnnualCost()).append("\n");
            }

            downloadFile("subscriptions_" + timestamp() + ".csv", csv.toString(), "text/csv");
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Exported " + subscriptions.size() + " subscriptions"));

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Export failed: " + e.getMessage()));
        }
    }

    public void exportPaymentsCsv() {
        try {
            Client user = userContext.getCurrentUser();
            if (user == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "User not logged in"));
                return;
            }

            List<PaymentHistory> payments = subscriptionService.getPaymentHistoryByClientId(user.getId());
            payments = filterPayments(payments);

            StringBuilder csv = new StringBuilder();
            csv.append("Date,Subscription,Amount,Currency,Status,Payment Method,Notes\n");

            for (PaymentHistory pay : payments) {
                csv.append(pay.getPaymentDate() != null ? pay.getPaymentDate().toString() : "").append(",");
                csv.append(escapeCsv(pay.getSubscription() != null ? pay.getSubscription().getName() : "")).append(",");
                csv.append(pay.getAmount()).append(",");
                csv.append(pay.getCurrency()).append(",");
                csv.append("").append(","); // Status
                csv.append("").append(","); // Payment Method
                csv.append(escapeCsv(pay.getNotes() != null ? pay.getNotes() : "")).append("\n");
            }

            downloadFile("payments_" + timestamp() + ".csv", csv.toString(), "text/csv");
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Exported " + payments.size() + " payments"));

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Export failed: " + e.getMessage()));
        }
    }

    public void requestFullBackup() {
        try {
            Client user = userContext.getCurrentUser();
            if (user == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "User not logged in"));
                return;
            }

            List<Subscription> subscriptions = subscriptionService.findByClientId(user.getId());
            List<PaymentHistory> payments = subscriptionService.getPaymentHistoryByClientId(user.getId());

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"exportDate\": \"").append(new Date().toString()).append("\",\n");
            json.append("  \"user\": {\n");
            json.append("    \"email\": \"").append(escapeJson(user.getEmail())).append("\",\n");
            json.append("    \"name\": \"").append(escapeJson(user.getFullName())).append("\"\n");
            json.append("  },\n");
            json.append("  \"subscriptions\": [\n");
            for (int i = 0; i < subscriptions.size(); i++) {
                Subscription sub = subscriptions.get(i);
                json.append("    {\n");
                json.append("      \"name\": \"").append(escapeJson(sub.getName())).append("\",\n");
                json.append("      \"price\": ").append(sub.getPrice()).append(",\n");
                json.append("      \"currency\": \"").append(sub.getOriginalCurrency()).append("\",\n");
                json.append("      \"frequency\": \"").append(sub.getFrequency()).append("\",\n");
                json.append("      \"status\": \"").append(sub.getStatus()).append("\",\n");
                json.append("      \"startDate\": \"").append(sub.getStartDate()).append("\",\n");
                json.append("      \"nextBillingDate\": \"").append(sub.getNextBillingDate()).append("\"\n");
                json.append("    }");
                if (i < subscriptions.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ],\n");
            json.append("  \"payments\": [\n");
            for (int i = 0; i < payments.size(); i++) {
                PaymentHistory pay = payments.get(i);
                json.append("    {\n");
                json.append("      \"date\": \"").append(pay.getPaymentDate()).append("\",\n");
                json.append("      \"subscription\": \"").append(escapeJson(pay.getSubscription() != null ? pay.getSubscription().getName() : "")).append("\",\n");
                json.append("      \"amount\": ").append(pay.getAmount()).append(",\n");
                json.append("      \"currency\": \"").append(pay.getCurrency()).append("\"\n");
                json.append("    }");
                if (i < payments.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ]\n");
            json.append("}\n");

            downloadFile("backup_" + timestamp() + ".json", json.toString(), "application/json");
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Backup ready for download"));

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Backup failed: " + e.getMessage()));
        }
    }

    private List<Subscription> filterSubscriptions(List<Subscription> subs) {
        List<Subscription> filtered = new ArrayList<>();
        for (Subscription sub : subs) {
            if ("all".equals(subStatus)) {
                filtered.add(sub);
            } else if (sub.getStatus() != null && sub.getStatus().name().equalsIgnoreCase(subStatus)) {
                filtered.add(sub);
            }
        }
        return filtered;
    }

    private List<PaymentHistory> filterPayments(List<PaymentHistory> payments) {
        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate cutoff;

        switch (dateRange) {
            case "month": cutoff = now.minusMonths(1); break;
            case "quarter": cutoff = now.minusMonths(3); break;
            case "year": cutoff = now.minusYears(1); break;
            default: return payments;
        }

        List<PaymentHistory> filtered = new ArrayList<>();
        for (PaymentHistory pay : payments) {
            if (pay.getPaymentDate() != null && pay.getPaymentDate().isAfter(cutoff)) {
                filtered.add(pay);
            }
        }
        return filtered;
    }

    private void downloadFile(String filename, String content, String contentType) throws IOException {
        FacesContext ctx = FacesContext.getCurrentInstance();
        jakarta.servlet.http.HttpServletResponse response = (jakarta.servlet.http.HttpServletResponse) 
            ctx.getExternalContext().getResponse();
        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.getOutputStream().write(content.getBytes());
        response.flushBuffer();
        ctx.responseComplete();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }

    private String timestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }

    public String getSubFormat() { return subFormat; }
    public void setSubFormat(String s) { this.subFormat = s; }
    public String getSubStatus() { return subStatus; }
    public void setSubStatus(String s) { this.subStatus = s; }
    public String getPayFormat() { return payFormat; }
    public void setPayFormat(String s) { this.payFormat = s; }
    public String getDateRange() { return dateRange; }
    public void setDateRange(String s) { this.dateRange = s; }
    public String getBackupFormat() { return backupFormat; }
    public void setBackupFormat(String s) { this.backupFormat = s; }
}