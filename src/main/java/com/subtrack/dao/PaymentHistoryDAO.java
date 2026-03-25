package com.subtrack.dao;

import com.subtrack.entity.PaymentHistory;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentHistoryDAO {
    
    void create(PaymentHistory paymentHistory);
    
    void update(PaymentHistory paymentHistory);
    
    void delete(PaymentHistory paymentHistory);
    
    Optional<PaymentHistory> findById(UUID id);
    
    List<PaymentHistory> findAll();
    
    List<PaymentHistory> findBySubscriptionId(UUID subscriptionId);
    
    List<PaymentHistory> findByDateRange(LocalDate startDate, LocalDate endDate);
    
    List<PaymentHistory> findBySubscriptionIdAndDateRange(UUID subscriptionId, LocalDate startDate, LocalDate endDate);
}
