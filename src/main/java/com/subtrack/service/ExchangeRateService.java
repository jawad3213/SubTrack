package com.subtrack.service;

import com.subtrack.dao.ExchangeRateDAO;
import com.subtrack.dao.PaymentHistoryDAO;
import com.subtrack.entity.Category;
import com.subtrack.entity.ExchangeRate;
import com.subtrack.entity.PaymentHistory;
import com.subtrack.entity.Subscription;
import com.subtrack.enums.SubscriptionStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ExchangeRateService {

    @Inject
    private ExchangeRateDAO exchangeRateDAO;
    
    @Inject
    private SubscriptionService subscriptionService;
    
    @Inject
    private CategoryService categoryService;
    
    @Inject
    private PaymentHistoryDAO paymentHistoryDAO;

    public List<ExchangeRate> findAll() {
        return exchangeRateDAO.findAll();
    }

    public Optional<ExchangeRate> findByCurrencies(String fromCurrency, String toCurrency) {
        return exchangeRateDAO.findByCurrencies(fromCurrency, toCurrency);
    }

    @Transactional
    public void save(ExchangeRate exchangeRate) {
        Optional<ExchangeRate> existing = exchangeRateDAO.findByCurrencies(
            exchangeRate.getFromCurrency(), exchangeRate.getToCurrency());
        if (existing.isPresent()) {
            existing.get().setRate(exchangeRate.getRate());
            exchangeRateDAO.update(existing.get());
        } else {
            exchangeRateDAO.create(exchangeRate);
        }
    }

    public BigDecimal convertToLocalCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null || amount == null) {
            return amount;
        }
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }
        
        Optional<ExchangeRate> rate = exchangeRateDAO.findByCurrencies(fromCurrency, toCurrency);
        if (rate.isPresent()) {
            return amount.multiply(rate.get().getRate());
        }
        
        Optional<ExchangeRate> inverseRate = exchangeRateDAO.findByCurrencies(toCurrency, fromCurrency);
        if (inverseRate.isPresent()) {
            return amount.divide(inverseRate.get().getRate(), 2, java.math.RoundingMode.HALF_UP);
        }
        
        return amount;
    }
    
    public Map<String, BigDecimal> getSpendingByCategory(String localCurrency, UUID clientId) {
        Map<String, BigDecimal> spending = new HashMap<>();
        List<Category> categories = categoryService.findAll();
        
        for (Category category : categories) {
            if (category.getId() != null) {
                List<Subscription> subs = subscriptionService.findByClientId(clientId);
                BigDecimal total = subs.stream()
                    .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                    .filter(s -> s.getCategory() != null && s.getCategory().getId().equals(category.getId()))
                    .map(s -> convertToLocalCurrency(s.getMonthlyCost(), s.getOriginalCurrency(), localCurrency))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                if (total.compareTo(BigDecimal.ZERO) > 0) {
                    spending.put(category.getName(), total);
                }
            }
        }
        
        return spending;
    }
    
    public List<MonthlySpending> getMonthlyTrend(UUID clientId, int months) {
        java.util.List<MonthlySpending> trend = new java.util.ArrayList<>();
        LocalDate now = LocalDate.now();
        
        List<Subscription> subscriptions = subscriptionService.findByClientId(clientId);
        
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            String monthName = monthDate.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH);
            
            LocalDate startOfMonth = monthDate.withDayOfMonth(1);
            LocalDate endOfMonth = monthDate.withDayOfMonth(monthDate.lengthOfMonth());
            
            BigDecimal monthlyCost = BigDecimal.ZERO;
            
            for (Subscription sub : subscriptions) {
                List<PaymentHistory> payments = paymentHistoryDAO.findBySubscriptionIdAndDateRange(
                    sub.getId(), startOfMonth, endOfMonth);
                for (PaymentHistory payment : payments) {
                    monthlyCost = monthlyCost.add(payment.getAmount());
                }
            }
            
            if (monthlyCost.compareTo(BigDecimal.ZERO) == 0) {
                monthlyCost = subscriptionService.calculateMonthlyCost(clientId);
            }
            
            trend.add(new MonthlySpending(monthName, monthlyCost));
        }
        
        return trend;
    }
    
    public static class MonthlySpending {
        private String month;
        private BigDecimal amount;
        
        public MonthlySpending(String month, BigDecimal amount) {
            this.month = month;
            this.amount = amount;
        }
        
        public String getMonth() { return month; }
        public BigDecimal getAmount() { return amount; }
    }
}
