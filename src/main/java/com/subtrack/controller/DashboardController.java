package com.subtrack.controller;

import com.subtrack.entity.Category;
import com.subtrack.entity.Client;
import com.subtrack.entity.Subscription;
import com.subtrack.enums.SubscriptionStatus;
import com.subtrack.service.CategoryService;
import com.subtrack.service.SubscriptionService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import com.subtrack.service.ExchangeRateService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Named
@ViewScoped
public class DashboardController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SubscriptionService subscriptionService;
    
    @Inject
    private CategoryService categoryService;
    
    @Inject
    private ExchangeRateService exchangeRateService;
    
    @Inject
    private UserContext userContext;

    private List<Subscription> recentSubscriptions;
    private BigDecimal totalMonthlyCost;
    private BigDecimal totalAnnualCost;
    private int activeSubscriptionCount;
    private List<CategoryBreakdown> categoryBreakdown;

    public void loadDashboard() {
        Client user = userContext.getCurrentUser();
        if (user == null) {
            return;
        }

        // Load recent subscriptions (limit to 5)
        recentSubscriptions = subscriptionService.findByClientId(user.getId());
        if (recentSubscriptions.size() > 5) {
            recentSubscriptions = recentSubscriptions.subList(0, 5);
        }

        // Calculate totals
        calculateTotals();

        // Calculate category breakdown
        calculateCategoryBreakdown();
    }

    private void calculateTotals() {
        Client user = userContext.getCurrentUser();
        List<Subscription> allSubscriptions = subscriptionService.findByClientId(user.getId());
        
        totalMonthlyCost = BigDecimal.ZERO;
        totalAnnualCost = BigDecimal.ZERO;
        activeSubscriptionCount = 0;

        for (Subscription sub : allSubscriptions) {
            if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
                activeSubscriptionCount++;
                totalMonthlyCost = totalMonthlyCost.add(sub.getMonthlyCost());
                totalAnnualCost = totalAnnualCost.add(sub.getAnnualCost());
            }
        }
    }

    private void calculateCategoryBreakdown() {
        categoryBreakdown = new ArrayList<>();
        Client user = userContext.getCurrentUser();
        List<Subscription> allSubscriptions = subscriptionService.findByClientId(user.getId());
        List<Category> categories = categoryService.findAll();

        for (Category cat : categories) {
            int count = 0;
            BigDecimal monthlyCost = BigDecimal.ZERO;

            for (Subscription sub : allSubscriptions) {
                if (sub.getStatus() == SubscriptionStatus.ACTIVE 
                    && sub.getCategory() != null 
                    && sub.getCategory().getId().equals(cat.getId())) {
                    count++;
                    monthlyCost = monthlyCost.add(sub.getMonthlyCost());
                }
            }

            if (count > 0) {
                CategoryBreakdown cb = new CategoryBreakdown();
                cb.setCategoryName(cat.getName());
                cb.setCount(count);
                cb.setMonthlyCost(monthlyCost);
                categoryBreakdown.add(cb);
            }
        }
    }

    // Getters
    public List<Subscription> getRecentSubscriptions() {
        if (recentSubscriptions == null) {
            loadDashboard();
        }
        return recentSubscriptions;
    }

    public BigDecimal getTotalMonthlyCost() {
        if (totalMonthlyCost == null) {
            calculateTotals();
        }
        return totalMonthlyCost;
    }

    public BigDecimal getTotalAnnualCost() {
        if (totalAnnualCost == null) {
            calculateTotals();
        }
        return totalAnnualCost;
    }

    public int getActiveSubscriptionCount() {
        if (activeSubscriptionCount == 0) {
            calculateTotals();
        }
        return activeSubscriptionCount;
    }

    public List<CategoryBreakdown> getCategoryBreakdown() {
        if (categoryBreakdown == null) {
            calculateCategoryBreakdown();
        }
        return categoryBreakdown;
    }
    
    public BigDecimal getConvertedMonthlyCost() {
        if (userContext.getClientId() != null) {
            return exchangeRateService.convertToLocalCurrency(
                getTotalMonthlyCost(), "USD", getUserCurrency());
        }
        return getTotalMonthlyCost();
    }
    
    public BigDecimal getConvertedAnnualCost() {
        if (userContext.getClientId() != null) {
            return exchangeRateService.convertToLocalCurrency(
                getTotalAnnualCost(), "USD", getUserCurrency());
        }
        return getTotalAnnualCost();
    }
    
    public String getUserCurrency() {
        return "USD";
    }
    
    public List<ExchangeRateService.MonthlySpending> getMonthlyTrend() {
        if (userContext.getClientId() != null) {
            return exchangeRateService.getMonthlyTrend(userContext.getClientId(), 6);
        }
        return new ArrayList<>();
    }
    
    public Map<String, BigDecimal> getSpendingByCategory() {
        if (userContext.getClientId() != null) {
            return exchangeRateService.getSpendingByCategory(getUserCurrency(), userContext.getClientId());
        }
        return new java.util.HashMap<>();
    }
    
    public List<SubscriptionService.OptimizationSuggestion> getOptimizationSuggestions() {
        if (userContext.getClientId() != null) {
            List<SubscriptionService.OptimizationSuggestion> all = new ArrayList<>();
            all.addAll(subscriptionService.detectRedundantSubscriptions(userContext.getClientId()));
            all.addAll(subscriptionService.detectUnusedSubscriptions(userContext.getClientId()));
            return all;
        }
        return new ArrayList<>();
    }
    
    public BigDecimal getTotalPotentialSavings() {
        List<SubscriptionService.OptimizationSuggestion> suggestions = getOptimizationSuggestions();
        BigDecimal total = BigDecimal.ZERO;
        for (SubscriptionService.OptimizationSuggestion s : suggestions) {
            total = total.add(s.getPotentialSavings());
        }
        return total;
    }

    // Inner class for category breakdown
    public static class CategoryBreakdown {
        private String categoryName;
        private int count;
        private BigDecimal monthlyCost;

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public BigDecimal getMonthlyCost() {
            return monthlyCost;
        }

        public void setMonthlyCost(BigDecimal monthlyCost) {
            this.monthlyCost = monthlyCost;
        }
    }
}
