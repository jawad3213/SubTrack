package com.subtrack.controller;

import com.subtrack.service.ClientService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@RequestScoped
public class AdminDashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ClientService clientService;

    @Inject
    private UserContext userContext;

    private long totalUsers;
    private long totalActiveSubscriptions;
    private double monthlyRecurringRevenue;
    private long newUsersThisMonth;
    private List<ActivityItem> recentActivities = new ArrayList<>();

    @PostConstruct
    public void init() {
        if (userContext.isAdmin()) {
            try {
                var allUsers = clientService.findAll();
                totalUsers = allUsers != null ? allUsers.size() : 0;

                totalActiveSubscriptions = 0;
                monthlyRecurringRevenue = 0.0;

                if (allUsers != null) {
                    for (var user : allUsers) {
                        if (user.getSubscriptions() != null) {
                            for (var sub : user.getSubscriptions()) {
                                if (sub.getStatus() != null && "ACTIVE".equals(sub.getStatus().name())) {
                                    totalActiveSubscriptions++;
                                    if (sub.getMonthlyCost() != null) {
                                        monthlyRecurringRevenue += sub.getMonthlyCost().doubleValue();
                                    }
                                }
                            }
                        }
                    }
                }

                // Count new users this month
                newUsersThisMonth = 0;
                if (allUsers != null) {
                    java.time.LocalDateTime startOfMonth = java.time.LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                    for (var user : allUsers) {
                        if (user.getCreatedAt() != null && user.getCreatedAt().isAfter(startOfMonth)) {
                            newUsersThisMonth++;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println(">>> AdminDashboardBean init error: " + e.getMessage());
            }
        }
    }

    // Getters
    public long getTotalUsers() { return totalUsers; }
    public long getTotalActiveSubscriptions() { return totalActiveSubscriptions; }
    public double getMonthlyRecurringRevenue() { return monthlyRecurringRevenue; }
    public long getNewUsersThisMonth() { return newUsersThisMonth; }
    public List<ActivityItem> getRecentActivities() { return recentActivities; }

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
