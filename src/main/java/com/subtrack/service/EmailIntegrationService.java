package com.subtrack.service;

import com.subtrack.dao.EmailIntegrationDAO;
import com.subtrack.entity.Client;
import com.subtrack.entity.EmailIntegration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class EmailIntegrationService {

    @Inject
    private EmailIntegrationDAO emailIntegrationDAO;

    public Optional<EmailIntegration> findByClientId(UUID clientId) {
        return emailIntegrationDAO.findByClientId(clientId);
    }

    public Optional<EmailIntegration> findById(UUID id) {
        return emailIntegrationDAO.findById(id);
    }

    @Transactional
    public void connectEmail(Client client, String emailAddress, String accessToken, String refreshToken, LocalDateTime expiresAt) {
        Optional<EmailIntegration> existing = emailIntegrationDAO.findByClientId(client.getId());
        
        EmailIntegration integration;
        if (existing.isPresent()) {
            integration = existing.get();
            integration.setEmailAddress(emailAddress);
            integration.setAccessToken(accessToken);
            integration.setRefreshToken(refreshToken);
            integration.setTokenExpiresAt(expiresAt);
            integration.setIsActive(true);
            emailIntegrationDAO.update(integration);
        } else {
            integration = new EmailIntegration();
            integration.setClient(client);
            integration.setEmailAddress(emailAddress);
            integration.setAccessToken(accessToken);
            integration.setRefreshToken(refreshToken);
            integration.setTokenExpiresAt(expiresAt);
            integration.setIsActive(true);
            emailIntegrationDAO.create(integration);
        }
    }

    @Transactional
    public void disconnectEmail(UUID clientId) {
        Optional<EmailIntegration> existing = emailIntegrationDAO.findByClientId(clientId);
        if (existing.isPresent()) {
            EmailIntegration integration = existing.get();
            integration.setIsActive(false);
            integration.setAccessToken(null);
            integration.setRefreshToken(null);
            emailIntegrationDAO.update(integration);
        }
    }

    @Transactional
    public void refreshToken(UUID clientId, String newAccessToken, LocalDateTime expiresAt) {
        Optional<EmailIntegration> existing = emailIntegrationDAO.findByClientId(clientId);
        if (existing.isPresent()) {
            EmailIntegration integration = existing.get();
            integration.setAccessToken(newAccessToken);
            integration.setTokenExpiresAt(expiresAt);
            emailIntegrationDAO.update(integration);
        }
    }
    
    public boolean isTokenValid(UUID clientId) {
        Optional<EmailIntegration> existing = emailIntegrationDAO.findByClientId(clientId);
        if (existing.isEmpty()) {
            return false;
        }
        EmailIntegration integration = existing.get();
        return integration.getIsActive() != null && integration.getIsActive() && !integration.isTokenExpired();
    }
}
