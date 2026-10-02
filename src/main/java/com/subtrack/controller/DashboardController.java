package com.subtrack.controller;

import com.subtrack.entity.Subscription;
import com.subtrack.enums.Frequency;
import com.subtrack.enums.SubscriptionStatus;
import com.subtrack.service.SubscriptionService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Backs the user dashboard, analytics and profile pages. Everything is computed once per view,
 * with all amounts converted to the display currency so they can be summed and charted.
 */
@Named
@ViewScoped
public class DashboardController implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Palette for categories without their own color (and for "Uncategorized"). */
    private static final String[] FALLBACK_COLORS = {
        "#185FA5", "#639922", "#BA7517", "#E24B4A", "#378ADD", "#85B7EB", "#5F5E5A", "#0C447C"
    };
    private static final int UPCOMING_DAYS = 30;
    private static final int TREND_MONTHS = 6;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private UserContext userContext;

    private String userCurrency;
    private List<Subscription> allSubscriptions = new ArrayList<>();
    private List<Subscription> recentSubscriptions = new ArrayList<>();
    private List<RankedSubscription> activeByCost = new ArrayList<>();
    private List<UpcomingRenewal> upcomingRenewals = new ArrayList<>();
    private List<CategoryBreakdown> categoryBreakdown = new ArrayList<>();
    private List<TrendBar> trend = new ArrayList<>();
    private List<FrequencyShare> frequencyMix = new ArrayList<>();
    private List<SubscriptionService.OptimizationSuggestion> optimizationSuggestions = new ArrayList<>();
    private BigDecimal totalMonthlyCost = BigDecimal.ZERO;
    private BigDecimal totalAnnualCost = BigDecimal.ZERO;
    private BigDecimal totalPotentialSavings = BigDecimal.ZERO;
    private int activeSubscriptionCount;
    private int pausedCount;
    private int cancelledCount;

    @PostConstruct
    public void init() {
        UUID clientId = userContext.getClientId();
        userCurrency = subscriptionService.getDisplayCurrency();
        if (clientId == null) {
            return;
        }

        allSubscriptions = subscriptionService.findByClientId(clientId);
        recentSubscriptions = allSubscriptions.size() > 5 ? allSubscriptions.subList(0, 5) : allSubscriptions;

        List<RankedSubscription> active = new ArrayList<>();
        for (Subscription sub : allSubscriptions) {
            if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
                BigDecimal monthly = subscriptionService.monthlyCostInDisplayCurrency(sub);
                active.add(new RankedSubscription(sub, monthly));
                totalMonthlyCost = totalMonthlyCost.add(monthly);
                totalAnnualCost = totalAnnualCost.add(perPeriod(monthly, sub.getFrequency(), true));
            } else if (sub.getStatus() == SubscriptionStatus.PAUSED) {
                pausedCount++;
            } else if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
                cancelledCount++;
            }
        }
        activeSubscriptionCount = active.size();

        active.sort(Comparator.comparing(RankedSubscription::getMonthlyCost).reversed());
        BigDecimal maxCost = active.isEmpty() ? BigDecimal.ZERO : active.get(0).getMonthlyCost();
        for (RankedSubscription r : active) {
            r.percentOfMax = percent(r.monthlyCost, maxCost);
            r.percentOfTotal = percent(r.monthlyCost, totalMonthlyCost);
        }
        activeByCost = active;

        buildUpcomingRenewals(active);
        buildCategoryBreakdown(active);
        buildTrend();
        buildFrequencyMix(active);

        optimizationSuggestions.addAll(subscriptionService.detectRedundantSubscriptions(clientId));
        optimizationSuggestions.addAll(subscriptionService.detectUnusedSubscriptions(clientId));
        for (SubscriptionService.OptimizationSuggestion s : optimizationSuggestions) {
            totalPotentialSavings = totalPotentialSavings.add(s.getPotentialSavings());
        }
    }

    private void buildUpcomingRenewals(List<RankedSubscription> active) {
        LocalDate today = LocalDate.now();
        for (RankedSubscription r : active) {
            LocalDate next = r.subscription.getNextBillingDate();
            if (next != null && !next.isBefore(today) && !next.isAfter(today.plusDays(UPCOMING_DAYS))) {
                upcomingRenewals.add(new UpcomingRenewal(r.subscription, ChronoUnit.DAYS.between(today, next),
                    perPeriod(r.monthlyCost, r.subscription.getFrequency(), false)));
            }
        }
        upcomingRenewals.sort(Comparator.comparingLong(UpcomingRenewal::getDaysUntil));
    }

    private void buildCategoryBreakdown(List<RankedSubscription> active) {
        Map<String, CategoryBreakdown> byName = new LinkedHashMap<>();
        for (RankedSubscription r : active) {
            var category = r.subscription.getCategory();
            String name = category != null ? category.getName() : "Uncategorized";
            CategoryBreakdown cb = byName.computeIfAbsent(name, n -> {
                CategoryBreakdown created = new CategoryBreakdown();
                created.categoryName = n;
                created.color = category != null ? category.getColor() : null;
                return created;
            });
            cb.count++;
            cb.monthlyCost = cb.monthlyCost.add(r.monthlyCost);
        }

        categoryBreakdown = new ArrayList<>(byName.values());
        categoryBreakdown.sort(Comparator.comparing(CategoryBreakdown::getMonthlyCost).reversed());
        int i = 0;
        for (CategoryBreakdown cb : categoryBreakdown) {
            if (cb.color == null || cb.color.isBlank()) {
                cb.color = FALLBACK_COLORS[i % FALLBACK_COLORS.length];
            }
            cb.percent = percent(cb.monthlyCost, totalMonthlyCost);
            i++;
        }
    }

    /**
     * Estimated spend for each of the last months: the subscriptions that had started by the end of
     * that month and are not cancelled. (Payment history isn't recorded, so this is an estimate.)
     */
    private void buildTrend() {
        YearMonth current = YearMonth.now();
        BigDecimal max = BigDecimal.ZERO;
        for (int i = TREND_MONTHS - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            LocalDate endOfMonth = month.atEndOfMonth();
            BigDecimal amount = BigDecimal.ZERO;
            for (Subscription sub : allSubscriptions) {
                boolean started = sub.getStartDate() == null || !sub.getStartDate().isAfter(endOfMonth);
                if (started && sub.getStatus() != SubscriptionStatus.CANCELLED) {
                    amount = amount.add(subscriptionService.monthlyCostInDisplayCurrency(sub));
                }
            }
            String label = month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            trend.add(new TrendBar(label, amount, i == 0));
            max = max.max(amount);
        }
        for (TrendBar bar : trend) {
            bar.heightPercent = max.signum() == 0 ? 0 : Math.max(2, percent(bar.amount, max));
        }
    }

    private void buildFrequencyMix(List<RankedSubscription> active) {
        Map<Frequency, Integer> counts = new EnumMap<>(Frequency.class);
        for (RankedSubscription r : active) {
            counts.merge(r.subscription.getFrequency(), 1, Integer::sum);
        }
        counts.forEach((freq, count) ->
            frequencyMix.add(new FrequencyShare(freq.getDisplayName(), count, percent(BigDecimal.valueOf(count),
                BigDecimal.valueOf(activeSubscriptionCount)))));
    }

    /** Converts a monthly cost to the cost per year (annual=true) or per billing period. */
    private static BigDecimal perPeriod(BigDecimal monthly, Frequency frequency, boolean annual) {
        if (frequency == null) {
            return BigDecimal.ZERO;
        }
        double factor = annual ? frequency.getAnnualMultiplier() : 1.0;
        return monthly.multiply(BigDecimal.valueOf(factor))
            .divide(BigDecimal.valueOf(frequency.getMonthlyMultiplier()), 2, RoundingMode.HALF_UP);
    }

    private static int percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() == 0 || part == null) {
            return 0;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 0, RoundingMode.HALF_UP).intValue();
    }

    // ----- Values for the pages -----

    public boolean isHasSubscriptions() { return !allSubscriptions.isEmpty(); }
    public String getUserCurrency() { return userCurrency; }
    public List<Subscription> getRecentSubscriptions() { return recentSubscriptions; }
    public BigDecimal getTotalMonthlyCost() { return totalMonthlyCost; }
    public BigDecimal getTotalAnnualCost() { return totalAnnualCost; }
    public int getActiveSubscriptionCount() { return activeSubscriptionCount; }
    public int getPausedCount() { return pausedCount; }
    public int getCancelledCount() { return cancelledCount; }
    public int getTotalSubscriptionCount() { return allSubscriptions.size(); }
    public List<UpcomingRenewal> getUpcomingRenewals() { return upcomingRenewals; }
    public List<CategoryBreakdown> getCategoryBreakdown() { return categoryBreakdown; }
    public List<TrendBar> getTrend() { return trend; }
    public List<FrequencyShare> getFrequencyMix() { return frequencyMix; }
    public List<SubscriptionService.OptimizationSuggestion> getOptimizationSuggestions() { return optimizationSuggestions; }
    public BigDecimal getTotalPotentialSavings() { return totalPotentialSavings; }

    /** All active subscriptions, most expensive first. */
    public List<RankedSubscription> getActiveByCost() { return activeByCost; }

    /** Active subscriptions, most expensive first (at most 5). */
    public List<RankedSubscription> getTopSubscriptions() {
        return activeByCost.size() > 5 ? activeByCost.subList(0, 5) : activeByCost;
    }

    public BigDecimal getAverageMonthlyCost() {
        return activeSubscriptionCount == 0 ? BigDecimal.ZERO
            : totalMonthlyCost.divide(BigDecimal.valueOf(activeSubscriptionCount), 2, RoundingMode.HALF_UP);
    }

    /** Amount due in the next 30 days. */
    public BigDecimal getUpcomingTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (UpcomingRenewal r : upcomingRenewals) {
            total = total.add(r.getAmountInDisplayCurrency());
        }
        return total;
    }

    public UpcomingRenewal getNextRenewal() {
        return upcomingRenewals.isEmpty() ? null : upcomingRenewals.get(0);
    }

    /** CSS conic-gradient for the category donut chart. */
    public String getCategoryDonutGradient() {
        if (categoryBreakdown.isEmpty()) {
            return "conic-gradient(#E6F1FB 0 100%)";
        }
        StringBuilder sb = new StringBuilder("conic-gradient(");
        int start = 0;
        for (int i = 0; i < categoryBreakdown.size(); i++) {
            CategoryBreakdown cb = categoryBreakdown.get(i);
            int end = i == categoryBreakdown.size() - 1 ? 100 : Math.min(100, start + cb.percent);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(cb.color).append(' ').append(start).append("% ").append(end).append('%');
            start = end;
        }
        return sb.append(')').toString();
    }

    // ----- View models -----

    public static class UpcomingRenewal implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Subscription subscription;
        private final long daysUntil;
        private final BigDecimal amountInDisplayCurrency;

        UpcomingRenewal(Subscription subscription, long daysUntil, BigDecimal amountInDisplayCurrency) {
            this.subscription = subscription;
            this.daysUntil = daysUntil;
            this.amountInDisplayCurrency = amountInDisplayCurrency;
        }

        public Subscription getSubscription() { return subscription; }
        public long getDaysUntil() { return daysUntil; }

        public String getWhen() {
            return daysUntil == 0 ? "Today" : daysUntil == 1 ? "Tomorrow" : "In " + daysUntil + " days";
        }

        /** "soon" within 3 days, used for highlighting. */
        public boolean isSoon() { return daysUntil <= 3; }

        /** One billing period's price, in the display currency. */
        public BigDecimal getAmountInDisplayCurrency() { return amountInDisplayCurrency; }
    }

    public static class RankedSubscription implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Subscription subscription;
        private final BigDecimal monthlyCost;
        private int percentOfMax;
        private int percentOfTotal;

        RankedSubscription(Subscription subscription, BigDecimal monthlyCost) {
            this.subscription = subscription;
            this.monthlyCost = monthlyCost;
        }

        public Subscription getSubscription() { return subscription; }
        public BigDecimal getMonthlyCost() { return monthlyCost; }
        public int getPercentOfMax() { return percentOfMax; }
        public int getPercentOfTotal() { return percentOfTotal; }
    }

    public static class CategoryBreakdown implements Serializable {
        private static final long serialVersionUID = 1L;
        private String categoryName;
        private String color;
        private int count;
        private int percent;
        private BigDecimal monthlyCost = BigDecimal.ZERO;

        public String getCategoryName() { return categoryName; }
        public String getColor() { return color; }
        public int getCount() { return count; }
        public int getPercent() { return percent; }
        public BigDecimal getMonthlyCost() { return monthlyCost; }
    }

    public static class TrendBar implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String month;
        private final BigDecimal amount;
        private final boolean current;
        private int heightPercent;

        TrendBar(String month, BigDecimal amount, boolean current) {
            this.month = month;
            this.amount = amount;
            this.current = current;
        }

        public String getMonth() { return month; }
        public BigDecimal getAmount() { return amount; }
        public boolean isCurrent() { return current; }
        public int getHeightPercent() { return heightPercent; }
    }

    public static class FrequencyShare implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String label;
        private final int count;
        private final int percent;

        FrequencyShare(String label, int count, int percent) {
            this.label = label;
            this.count = count;
            this.percent = percent;
        }

        public String getLabel() { return label; }
        public int getCount() { return count; }
        public int getPercent() { return percent; }
    }
}
