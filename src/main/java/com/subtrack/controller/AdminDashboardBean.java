package com.subtrack.controller;

import com.subtrack.service.ClientService;
import com.subtrack.service.SubscriptionService;
import com.subtrack.service.AlertService;
import com.subtrack.service.InvoiceService;
import com.subtrack.service.SaaSCatalogService;
import com.subtrack.dao.SystemConfigDAO;
import com.subtrack.entity.Client;
import com.subtrack.entity.Subscription;
import com.subtrack.entity.AlertRule;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Named
@RequestScoped
public class AdminDashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ClientService clientService;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private AlertService alertService;

    @Inject
    private InvoiceService invoiceService;

    @Inject
    private SystemConfigDAO systemConfigDAO;

    @Inject
    private UserContext userContext;

    @Inject
    private SaaSCatalogService saasCatalogService;

    private long totalUsers;
    private long totalCatalogServices;
    private long totalActiveSubscriptions;
    private double monthlyRecurringRevenue;
    private long newUsersThisMonth;
    private List<ActivityItem> recentActivities = new ArrayList<>();
    
    private boolean exchangeRateConfigured;
    private boolean n8nWebhookConfigured;
    private boolean geminiConfigured;

    @PostConstruct
    public void init() {
        checkApiConfigurations();
        
        if (userContext.isAdmin()) {
            try {
                List<Client> allUsers = clientService.findAll();
                totalUsers = allUsers != null ? allUsers.size() : 0;
                
                totalCatalogServices = saasCatalogService.getAllServices().size();

                totalActiveSubscriptions = 0;
                monthlyRecurringRevenue = 0.0;
                List<Subscription> allSubscriptions = new ArrayList<>();

                if (allUsers != null) {
                    for (Client user : allUsers) {
                        List<Subscription> userSubs = subscriptionService.findByClientId(user.getId());
                        allSubscriptions.addAll(userSubs);
                        for (Subscription sub : userSubs) {
                            if (sub.getStatus() != null && sub.getStatus().name().equals("ACTIVE")) {
                                totalActiveSubscriptions++;
                                if (sub.getMonthlyCost() != null) {
                                    monthlyRecurringRevenue += sub.getMonthlyCost().doubleValue();
                                }
                            }
                        }
                    }
                }

                LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                newUsersThisMonth = 0;
                if (allUsers != null) {
                    for (Client user : allUsers) {
                        if (user.getCreatedAt() != null && user.getCreatedAt().isAfter(startOfMonth)) {
                            newUsersThisMonth++;
                        }
                    }
                }

                recentActivities = buildActivityFeed(allUsers, allSubscriptions);
            } catch (Exception e) {
                System.err.println(">>> AdminDashboardBean init error: " + e.getMessage());
            }
        }
    }

    private void checkApiConfigurations() {
        String exchangeUrl = systemConfigDAO.getValue("EXCHANGE_RATE_URL", "");
        String webhookUrl = systemConfigDAO.getValue("N8N_WEBHOOK_URL", "");
        String geminiKey = systemConfigDAO.getValue("GEMINI_API_KEY", "");
        
        exchangeRateConfigured = exchangeUrl != null && !exchangeUrl.isBlank();
        n8nWebhookConfigured = webhookUrl != null && !webhookUrl.isBlank();
        geminiConfigured = geminiKey != null && !geminiKey.isBlank();
    }

    private List<ActivityItem> buildActivityFeed(List<Client> users, List<Subscription> subscriptions) {
        List<ActivityItem> activities = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (users != null) {
            for (Client user : users) {
                if (user.getCreatedAt() != null) {
                    long minutes = ChronoUnit.MINUTES.between(user.getCreatedAt(), now);
                    if (minutes < 10080) {
                        String timeAgo = formatTimeAgo(minutes);
                        activities.add(new ActivityItem("user", 
                            "New user registered: " + user.getEmail(), timeAgo));
                    }
                }
            }
        }

        if (subscriptions != null) {
            for (Subscription sub : subscriptions) {
                if (sub.getUpdatedAt() != null) {
                    long minutes = ChronoUnit.MINUTES.between(sub.getUpdatedAt(), now);
                    if (minutes < 10080) {
                        String timeAgo = formatTimeAgo(minutes);
                        String action = sub.getStatus() != null ? sub.getStatus().name().toLowerCase() : "updated";
                        activities.add(new ActivityItem("subscription",
                            "Subscription " + action + ": " + sub.getName(), timeAgo));
                    }
                }
            }
        }

        return activities.stream()
            .sorted((a, b) -> a.getTimeAgo().compareTo(b.getTimeAgo()))
            .limit(10)
            .collect(Collectors.toList());
    }

    private String formatTimeAgo(long minutes) {
        if (minutes < 60) return minutes + "m ago";
        if (minutes < 1440) return (minutes / 60) + "h ago";
        return (minutes / 1440) + "d ago";
    }

    // Getters
    public long getTotalUsers() { return totalUsers; }
    public long getTotalCatalogServices() { return totalCatalogServices; }
    public long getTotalActiveSubscriptions() { return totalActiveSubscriptions; }
    public double getMonthlyRecurringRevenue() { return monthlyRecurringRevenue; }
    public long getNewUsersThisMonth() { return newUsersThisMonth; }
    public List<ActivityItem> getRecentActivities() { return recentActivities; }
    public boolean isExchangeRateConfigured() { return exchangeRateConfigured; }
    public boolean isN8nWebhookConfigured() { return n8nWebhookConfigured; }
    public boolean isGeminiConfigured() { return geminiConfigured; }

    // Inner class for activity feed items
    public static class ActivityItem {
        private String type; // user, sub, invoice, alert
        private String message;
        private String timeAgo;

        public ActivityItem(String type, String message, String timeAgo) {
            this.type = type;
            this.message = message;
            this.timeAgo = timeAgo;
        }

        public String getType() { return type; }
        public String getMessage() { return message; }
        public String getTimeAgo() { return timeAgo; }
    }
}
