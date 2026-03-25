package com.subtrack.dao;

import com.subtrack.entity.SaaSService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaaSServiceDAO {
    
    void create(SaaSService service);
    
    void update(SaaSService service);
    
    void delete(SaaSService service);
    
    Optional<SaaSService> findById(UUID id);
    
    List<SaaSService> findAll();
    
    List<SaaSService> findByNameContaining(String name);
    
    Optional<SaaSService> findByServiceName(String serviceName);
}
