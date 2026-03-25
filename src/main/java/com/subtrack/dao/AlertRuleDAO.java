package com.subtrack.dao;

import com.subtrack.entity.AlertRule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRuleDAO {
    
    void create(AlertRule alertRule);
    
    void update(AlertRule alertRule);
    
    void delete(AlertRule alertRule);
    
    Optional<AlertRule> findById(UUID id);
    
    List<AlertRule> findAll();
    
    List<AlertRule> findBySubscriptionId(UUID subscriptionId);
    
    List<AlertRule> findActiveAlertRules();
    
    List<AlertRule> findAlertsDueSoon(int days);
}
