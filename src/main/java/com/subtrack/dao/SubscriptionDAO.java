package com.subtrack.dao;

import com.subtrack.entity.Subscription;
import com.subtrack.enums.SubscriptionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionDAO {
    
    void create(Subscription subscription);
    
    void update(Subscription subscription);
    
    void delete(Subscription subscription);
    
    Optional<Subscription> findById(UUID id);
    
    List<Subscription> findByClientId(UUID clientId);
    
    List<Subscription> findAll();
    
    List<Subscription> findByClientIdAndStatus(UUID clientId, SubscriptionStatus status);
    
    List<Subscription> findByClientIdAndCategoryId(UUID clientId, UUID categoryId);
    
    List<Subscription> findByNameContaining(String name);
}
