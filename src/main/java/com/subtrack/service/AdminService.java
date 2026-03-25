package com.subtrack.service;

import com.subtrack.dao.ClientDAO;
import com.subtrack.entity.Client;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AdminService {

    @Inject
    private ClientDAO clientDAO;

    public List<Client> getAllUsers() {
        return clientDAO.findAll();
    }

    public Client getUserById(UUID userId) {
        return clientDAO.findById(userId).orElse(null);
    }

    @Transactional
    public void suspendUser(UUID userId) {
        clientDAO.findById(userId).ifPresent(user -> {
            user.setIsActive(false);
            clientDAO.update(user);
        });
    }

    @Transactional
    public void activateUser(UUID userId) {
        clientDAO.findById(userId).ifPresent(user -> {
            user.setIsActive(true);
            clientDAO.update(user);
        });
    }

    @Transactional
    public void removeUser(UUID userId) {
        clientDAO.findById(userId).ifPresent(clientDAO::delete);
    }
}
