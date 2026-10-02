package com.subtrack.controller.admin;

import com.subtrack.dao.SystemConfigDAO;
import com.subtrack.entity.Client;
import com.subtrack.entity.Subscription;
import com.subtrack.enums.SubscriptionStatus;
import com.subtrack.service.ClientService;
import com.subtrack.service.SaaSCatalogService;
import com.subtrack.service.SubscriptionService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Platform-wide metrics for the admin dashboard. Access is restricted to admins by AuthenticationFilter. */
@Named
@RequestScoped
public class AdminDashboardBean implements Serializable {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AdminDashboardBean.class);

    private static final long serialVersionUID = 1L;
    private static final int TREND_MONTHS = 6;
    private static final long ACTIVITY_WINDOW_MINUTES = 7 * 24 * 60;

    @Inject
    private ClientService clientService;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private SystemConfigDAO systemConfigDAO;

    @Inject
    private SaaSCatalogService saasCatalogService;

    private long totalUsers;
    private long activeUsers;
    private long totalCatalogServices;
    private long newUsersThisMonth;
    private long totalSubscriptions;
    private long activeSubscriptions;
    private long pausedSubscriptions;
    private long cancelledSubscriptions;
    private BigDecimal platformMonthlySpend = BigDecimal.ZERO;
    private String displayCurrency;
    private List<SignupBar> signupTrend = new ArrayList<>();
    private List<ServiceCount> topServices = new ArrayList<>();
    private List<ActivityItem> recentActivities = new ArrayList<>();

    private boolean exchangeRateConfigured;
    private boolean n8nWebhookConfigured;
    private boolean geminiConfigured;

    @PostConstruct
    public void init() {
        checkApiConfigurations();
        displayCurrency = subscriptionService.getDisplayCurrency();
        try {
            List<Client> users = clientService.findAll();
            List<Subscription> subscriptions = subscriptionService.findAll();

            totalUsers = users.size();
            activeUsers = users.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();
            totalCatalogServices = saasCatalogService.getAllServices().size();
            totalSubscriptions = subscriptions.size();

            for (Subscription sub : subscriptions) {
                if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
                    activeSubscriptions++;
                    platformMonthlySpend = platformMonthlySpend.add(subscriptionService.monthlyCostInDisplayCurrency(sub));
                } else if (sub.getStatus() == SubscriptionStatus.PAUSED) {
                    pausedSubscriptions++;
                } else if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
                    cancelledSubscriptions++;
                }
            }

            buildSignupTrend(users);
            buildTopServices(subscriptions);
            recentActivities = buildActivityFeed(users, subscriptions);
        } catch (Exception e) {
            LOGGER.warn("AdminDashboardBean init error", e);
        }
    }

    private void checkApiConfigurations() {
        exchangeRateConfigured = isSet("EXCHANGE_RATE_URL");
        n8nWebhookConfigured = isSet("N8N_WEBHOOK_URL");
        geminiConfigured = isSet("GEMINI_API_KEY");
    }

    private boolean isSet(String key) {
        String value = systemConfigDAO.getValue(key, "");
        return value != null && !value.isBlank();
    }

    private void buildSignupTrend(List<Client> users) {
        YearMonth current = YearMonth.now();
        Map<YearMonth, Long> counts = new HashMap<>();
        for (Client user : users) {
            if (user.getCreatedAt() != null) {
                counts.merge(YearMonth.from(user.getCreatedAt()), 1L, Long::sum);
            }
        }
        newUsersThisMonth = counts.getOrDefault(current, 0L);

        long max = 0;
        for (int i = TREND_MONTHS - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            long count = counts.getOrDefault(month, 0L);
            signupTrend.add(new SignupBar(month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), count, i == 0));
            max = Math.max(max, count);
        }
        for (SignupBar bar : signupTrend) {
            bar.heightPercent = max == 0 ? 0 : (int) Math.max(2, Math.round(bar.count * 100.0 / max));
        }
    }

    /** Most-tracked services across all users, by subscription name (case-insensitive). */
    private void buildTopServices(List<Subscription> subscriptions) {
        Map<String, ServiceCount> byName = new HashMap<>();
        for (Subscription sub : subscriptions) {
            if (sub.getName() == null || sub.getName().isBlank()) {
                continue;
            }
            byName.computeIfAbsent(sub.getName().trim().toLowerCase(), k -> new ServiceCount(sub.getName().trim())).count++;
        }
        List<ServiceCount> sorted = new ArrayList<>(byName.values());
        sorted.sort(Comparator.comparingLong(ServiceCount::getCount).reversed().thenComparing(ServiceCount::getName));
        topServices = sorted.size() > 5 ? new ArrayList<>(sorted.subList(0, 5)) : sorted;
        long max = topServices.isEmpty() ? 0 : topServices.get(0).count;
        for (ServiceCount sc : topServices) {
            sc.percentOfMax = max == 0 ? 0 : (int) Math.round(sc.count * 100.0 / max);
        }
    }

    private List<ActivityItem> buildActivityFeed(List<Client> users, List<Subscription> subscriptions) {
        LocalDateTime now = LocalDateTime.now();
        List<ActivityItem> activities = new ArrayList<>();
        for (Client user : users) {
            if (user.getCreatedAt() != null) {
                long minutes = ChronoUnit.MINUTES.between(user.getCreatedAt(), now);
                if (minutes < ACTIVITY_WINDOW_MINUTES) {
                    activities.add(new ActivityItem("user", "New user registered: " + user.getEmail(), minutes));
                }
            }
        }
        for (Subscription sub : subscriptions) {
            if (sub.getUpdatedAt() != null) {
                long minutes = ChronoUnit.MINUTES.between(sub.getUpdatedAt(), now);
                if (minutes < ACTIVITY_WINDOW_MINUTES) {
                    String action = sub.getStatus() != null ? sub.getStatus().name().toLowerCase() : "updated";
                    activities.add(new ActivityItem("sub", "Subscription " + action + ": " + sub.getName(), minutes));
                }
            }
        }
        activities.sort(Comparator.comparingLong(ActivityItem::getMinutesAgo));
        return activities.size() > 10 ? new ArrayList<>(activities.subList(0, 10)) : activities;
    }

    private static int percent(long part, long whole) {
        return whole == 0 ? 0 : (int) Math.round(part * 100.0 / whole);
    }

    // Getters
    public long getTotalUsers() { return totalUsers; }
    public long getActiveUsers() { return activeUsers; }
    public long getSuspendedUsers() { return totalUsers - activeUsers; }
    public long getTotalCatalogServices() { return totalCatalogServices; }
    public long getNewUsersThisMonth() { return newUsersThisMonth; }
    public long getTotalSubscriptions() { return totalSubscriptions; }
    public long getActiveSubscriptions() { return activeSubscriptions; }
    public long getPausedSubscriptions() { return pausedSubscriptions; }
    public long getCancelledSubscriptions() { return cancelledSubscriptions; }
    public int getActivePercent() { return percent(activeSubscriptions, totalSubscriptions); }
    public int getPausedPercent() { return percent(pausedSubscriptions, totalSubscriptions); }
    public int getCancelledPercent() { return percent(cancelledSubscriptions, totalSubscriptions); }
    public BigDecimal getPlatformMonthlySpend() { return platformMonthlySpend; }
    public String getDisplayCurrency() { return displayCurrency; }
    public List<SignupBar> getSignupTrend() { return signupTrend; }
    public List<ServiceCount> getTopServices() { return topServices; }
    public List<ActivityItem> getRecentActivities() { return recentActivities; }
    public boolean isExchangeRateConfigured() { return exchangeRateConfigured; }
    public boolean isN8nWebhookConfigured() { return n8nWebhookConfigured; }
    public boolean isGeminiConfigured() { return geminiConfigured; }

    public BigDecimal getAverageSubscriptionsPerUser() {
        return totalUsers == 0 ? BigDecimal.ZERO
            : BigDecimal.valueOf(totalSubscriptions).divide(BigDecimal.valueOf(totalUsers), 1, RoundingMode.HALF_UP);
    }

    public static class SignupBar implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String month;
        private final long count;
        private final boolean current;
        private int heightPercent;

        SignupBar(String month, long count, boolean current) {
            this.month = month;
            this.count = count;
            this.current = current;
        }

        public String getMonth() { return month; }
        public long getCount() { return count; }
        public boolean isCurrent() { return current; }
        public int getHeightPercent() { return heightPercent; }
    }

    public static class ServiceCount implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String name;
        private long count;
        private int percentOfMax;

        ServiceCount(String name) {
            this.name = name;
        }

        public String getName() { return name; }
        public long getCount() { return count; }
        public int getPercentOfMax() { return percentOfMax; }
    }

    public static class ActivityItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String type; // user, sub
        private final String message;
        private final long minutesAgo;

        public ActivityItem(String type, String message, long minutesAgo) {
            this.type = type;
            this.message = message;
            this.minutesAgo = minutesAgo;
        }

        public String getType() { return type; }
        public String getMessage() { return message; }
        public long getMinutesAgo() { return minutesAgo; }

        public String getTimeAgo() {
            if (minutesAgo < 60) return minutesAgo + "m ago";
            if (minutesAgo < 1440) return (minutesAgo / 60) + "h ago";
            return (minutesAgo / 1440) + "d ago";
        }
    }
}
