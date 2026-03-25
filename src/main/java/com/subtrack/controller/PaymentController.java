package com.subtrack.controller;

import com.subtrack.entity.PaymentHistory;
import com.subtrack.entity.Subscription;
import com.subtrack.service.SubscriptionService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Named
@SessionScoped
public class PaymentController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private UserContext userContext;

    private List<PaymentHistory> allPayments;
    private List<PaymentHistory> filteredPayments;
    private Subscription selectedSubscription;
    private BigDecimal totalPaid;
    private LocalDate startDate;
    private LocalDate endDate;

    @PostConstruct
    public void init() {
        loadPayments();
    }

    public void loadPayments() {
        if (userContext.getClientId() != null) {
            allPayments = subscriptionService.getPaymentHistoryByClientId(userContext.getClientId());
            filteredPayments = new ArrayList<>(allPayments);
            calculateTotal();
        }
    }

    public void filterBySubscription() {
        if (selectedSubscription != null) {
            filteredPayments = allPayments.stream()
                .filter(p -> p.getSubscription().getId().equals(selectedSubscription.getId()))
                .collect(Collectors.toList());
        } else {
            filteredPayments = new ArrayList<>(allPayments);
        }
        calculateTotal();
    }

    public void filterByDateRange() {
        filteredPayments = allPayments.stream()
            .filter(p -> {
                if (startDate != null && p.getPaymentDate().isBefore(startDate)) {
                    return false;
                }
                if (endDate != null && p.getPaymentDate().isAfter(endDate)) {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
        calculateTotal();
    }

    public void clearFilters() {
        selectedSubscription = null;
        startDate = null;
        endDate = null;
        filteredPayments = new ArrayList<>(allPayments);
        calculateTotal();
    }

    private void calculateTotal() {
        totalPaid = filteredPayments.stream()
            .map(PaymentHistory::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Subscription> getSubscriptions() {
        return subscriptionService.findByClientId(userContext.getClientId());
    }

    public List<PaymentHistory> getAllPayments() {
        return allPayments;
    }

    public List<PaymentHistory> getFilteredPayments() {
        return filteredPayments;
    }

    public Subscription getSelectedSubscription() {
        return selectedSubscription;
    }

    public void setSelectedSubscription(Subscription selectedSubscription) {
        this.selectedSubscription = selectedSubscription;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
