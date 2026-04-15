package com.subtrack.service;

import com.subtrack.dao.PaymentHistoryDAO;
import com.subtrack.dao.SubscriptionDAO;
import com.subtrack.entity.PaymentHistory;
import com.subtrack.entity.Subscription;
import com.subtrack.enums.SubscriptionStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SubscriptionService {

    @Inject
    private SubscriptionDAO subscriptionDAO;
    
    @Inject
    private PaymentHistoryDAO paymentHistoryDAO;

    public List<Subscription> findAll() {
        return subscriptionDAO.findAll();
    }

    public List<Subscription> findByClientId(UUID clientId) {
        return subscriptionDAO.findByClientId(clientId);
    }
    
    public List<Subscription> findActiveByClientId(UUID clientId) {
        return subscriptionDAO.findByClientIdAndStatus(clientId, SubscriptionStatus.ACTIVE);
    }

    public Optional<Subscription> findById(UUID id) {
        return subscriptionDAO.findById(id);
    }

    @Transactional
    public void create(Subscription subscription) {
        subscriptionDAO.create(subscription);
    }

    @Transactional
    public void update(Subscription subscription) {
        subscriptionDAO.update(subscription);
    }

    @Transactional
    public void delete(Subscription subscription) {
        subscriptionDAO.delete(subscription);
    }
    
    public BigDecimal calculateMonthlyCost(UUID clientId) {
        List<Subscription> subscriptions = findActiveByClientId(clientId);
        BigDecimal total = BigDecimal.ZERO;
        for (Subscription sub : subscriptions) {
            total = total.add(sub.getMonthlyCost());
        }
        return total;
    }
    
    public BigDecimal calculateAnnualCost(UUID clientId) {
        List<Subscription> subscriptions = findActiveByClientId(clientId);
        BigDecimal total = BigDecimal.ZERO;
        for (Subscription sub : subscriptions) {
            total = total.add(sub.getAnnualCost());
        }
        return total;
    }
    
    @Transactional
    public void pauseSubscription(Subscription subscription) {
        subscription.setStatus(SubscriptionStatus.PAUSED);
        subscription.calculateNextBillingDate();
        subscriptionDAO.update(subscription);
    }
    
    @Transactional
    public void cancelSubscription(Subscription subscription) {
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setNextBillingDate(null);
        subscriptionDAO.update(subscription);
    }
    
    @Transactional
    public void reactivateSubscription(Subscription subscription) {
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.calculateNextBillingDate();
        subscriptionDAO.update(subscription);
    }
    
    @Transactional
    private void recordPayment(Subscription subscription) {
        PaymentHistory payment = new PaymentHistory();
        payment.setSubscription(subscription);
        payment.setAmount(subscription.getPrice());
        payment.setCurrency(subscription.getOriginalCurrency());
        payment.setPaymentDate(LocalDate.now());
        paymentHistoryDAO.create(payment);
    }
    
    public List<PaymentHistory> getPaymentHistoryBySubscriptionId(UUID subscriptionId) {
        return paymentHistoryDAO.findBySubscriptionId(subscriptionId);
    }
    
    public List<PaymentHistory> getPaymentHistoryByClientId(UUID clientId) {
        List<Subscription> subscriptions = subscriptionDAO.findByClientId(clientId);
        List<PaymentHistory> allPayments = new ArrayList<>();
        for (Subscription sub : subscriptions) {
            allPayments.addAll(paymentHistoryDAO.findBySubscriptionId(sub.getId()));
        }
        return allPayments;
    }
    
    public BigDecimal calculateTotalByCategory(UUID clientId, UUID categoryId) {
        List<Subscription> subscriptions = subscriptionDAO.findByClientIdAndCategoryId(clientId, categoryId);
        BigDecimal total = BigDecimal.ZERO;
        for (Subscription sub : subscriptions) {
            if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
                total = total.add(sub.getMonthlyCost());
            }
        }
        return total;
    }
    
    public List<OptimizationSuggestion> detectRedundantSubscriptions(UUID clientId) {
        List<OptimizationSuggestion> suggestions = new ArrayList<>();
        List<Subscription> subscriptions = findByClientId(clientId);
        
        Map<String, List<Subscription>> serviceGroups = new HashMap<>();
        for (Subscription sub : subscriptions) {
            String key = sub.getName().toLowerCase();
            serviceGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(sub);
        }
        
        for (Map.Entry<String, List<Subscription>> entry : serviceGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                List<Subscription> duplicates = entry.getValue();
                BigDecimal potentialSavings = BigDecimal.ZERO;
                for (int i = 1; i < duplicates.size(); i++) {
                    potentialSavings = potentialSavings.add(duplicates.get(i).getMonthlyCost());
                }
                suggestions.add(new OptimizationSuggestion(
                    "Duplicate subscription: " + duplicates.get(0).getName(),
                    "You have " + duplicates.size() + " subscriptions to the same service. Consider consolidating.",
                    potentialSavings,
                    duplicates
                ));
            }
        }
        
        return suggestions;
    }
    
    public List<OptimizationSuggestion> detectUnusedSubscriptions(UUID clientId) {
        List<OptimizationSuggestion> suggestions = new ArrayList<>();
        List<Subscription> subscriptions = findByClientId(clientId);
        
        for (Subscription sub : subscriptions) {
            if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
                List<PaymentHistory> payments = paymentHistoryDAO.findBySubscriptionId(sub.getId());
                if (payments.isEmpty() || payments.size() == 1) {
                    suggestions.add(new OptimizationSuggestion(
                        "Unused subscription: " + sub.getName(),
                        "No payment history found. Consider pausing or cancelling.",
                        sub.getMonthlyCost(),
                        List.of(sub)
                    ));
                }
            }
        }
        
        return suggestions;
    }
    
    public static class OptimizationSuggestion {
        private String title;
        private String description;
        private BigDecimal potentialSavings;
        private List<Subscription> affectedSubscriptions;
        
        public OptimizationSuggestion(String title, String description, BigDecimal potentialSavings, 
                                    List<Subscription> affectedSubscriptions) {
            this.title = title;
            this.description = description;
            this.potentialSavings = potentialSavings;
            this.affectedSubscriptions = affectedSubscriptions;
        }
        
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public BigDecimal getPotentialSavings() { return potentialSavings; }
        public List<Subscription> getAffectedSubscriptions() { return affectedSubscriptions; }
    }
}
