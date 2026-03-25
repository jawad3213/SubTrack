package com.subtrack.dao;

import com.subtrack.entity.Client;
import com.subtrack.enums.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientDAO {
    
    void create(Client client);
    
    void update(Client client);
    
    void delete(Client client);
    
    Optional<Client> findById(UUID id);
    
    Optional<Client> findByEmail(String email);
    
    List<Client> findAll();
    
    List<Client> findByRole(Role role);
    
    List<Client> findActiveClients();
    
    boolean existsByEmail(String email);
}
