package com.subtrack.dao;

import com.subtrack.entity.EmailIntegration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailIntegrationDAO {
    
    void create(EmailIntegration emailIntegration);
    
    void update(EmailIntegration emailIntegration);
    
    void delete(EmailIntegration emailIntegration);
    
    Optional<EmailIntegration> findById(UUID id);
    
    Optional<EmailIntegration> findByClientId(UUID clientId);
    
    List<EmailIntegration> findAll();
    
    List<EmailIntegration> findActiveIntegrations();
}
