package com.subtrack.service;

import com.subtrack.dao.ClientDAO;
import com.subtrack.entity.Client;
import com.subtrack.enums.AccountType;
import com.subtrack.enums.Role;
import com.subtrack.util.PasswordUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ClientService {

    @Inject
    private ClientDAO clientDAO;

    public Client registerClient(String email, String password, String firstName, 
                                  String lastName, AccountType accountType) {
        if (clientDAO.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        
        if (!PasswordUtil.isPasswordStrong(password)) {
            throw new IllegalArgumentException("Password must be at least 8 characters with uppercase, lowercase, and digits");
        }
        
        Client client = new Client();
        client.setEmail(email);
        client.setPassword(PasswordUtil.hashPassword(password));
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setAccountType(accountType != null ? accountType : AccountType.B2C);
        client.setRole(Role.CLIENT);
        client.setIsActive(true);
        
        clientDAO.create(client);
        return client;
    }

    public Optional<Client> authenticate(String email, String password) {
        System.out.println(">>> ClientService.authenticate called for email: " + email);
        Optional<Client> clientOpt = clientDAO.findByEmail(email);
        
        System.out.println(">>> Client found in DB: " + clientOpt.isPresent());
        if (clientOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Client client = clientOpt.get();
        
        if (client.getIsActive() == null || !client.getIsActive()) {
            System.out.println(">>> Client account is deactivated");
            throw new IllegalStateException("Account is deactivated");
        }
        
        boolean pwMatch = PasswordUtil.verifyPassword(password, client.getPassword());
        System.out.println(">>> Password match: " + pwMatch);
        if (pwMatch) {
            return Optional.of(client);
        }
        
        return Optional.empty();
    }

    public Optional<Client> findById(UUID id) {
        return clientDAO.findById(id);
    }

    public Optional<Client> findByEmail(String email) {
        return clientDAO.findByEmail(email);
    }

    public List<Client> findAll() {
        return clientDAO.findAll();
    }

    public List<Client> findAdmins() {
        return clientDAO.findByRole(Role.ADMIN);
    }

    public List<Client> findActiveClients() {
        return clientDAO.findActiveClients();
    }

    public void updateClient(Client client) {
        clientDAO.update(client);
    }

    public void deactivateClient(UUID id) {
        clientDAO.findById(id).ifPresent(client -> {
            client.setIsActive(false);
            clientDAO.update(client);
        });
    }

    public void activateClient(UUID id) {
        clientDAO.findById(id).ifPresent(client -> {
            client.setIsActive(true);
            clientDAO.update(client);
        });
    }

    public void deleteClient(UUID id) {
        clientDAO.findById(id).ifPresent(clientDAO::delete);
    }

    public boolean isEmailTaken(String email) {
        return clientDAO.existsByEmail(email);
    }
    
    public void update(Client client) {
        clientDAO.update(client);
    }
    
    public void changePassword(UUID clientId, String currentPassword, String newPassword) {
        Optional<Client> clientOpt = clientDAO.findById(clientId);
        if (clientOpt.isEmpty()) {
            throw new IllegalArgumentException("Client not found");
        }
        
        Client client = clientOpt.get();
        if (!PasswordUtil.verifyPassword(currentPassword, client.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        client.setPassword(PasswordUtil.hashPassword(newPassword));
        clientDAO.update(client);
    }
    
    public void updateNotificationPreferences(UUID clientId, boolean emailNotifications, 
                                              boolean telegramEnabled, String telegramChatId,
                                              boolean whatsappEnabled, String whatsappNumber) {
        Optional<Client> clientOpt = clientDAO.findById(clientId);
        if (clientOpt.isEmpty()) {
            throw new IllegalArgumentException("Client not found");
        }
        
        Client client = clientOpt.get();
        client.setEmailNotifications(emailNotifications);
        client.setTelegramEnabled(telegramEnabled);
        client.setTelegramChatId(telegramChatId);
        client.setWhatsappEnabled(whatsappEnabled);
        client.setWhatsappNumber(whatsappNumber);
        clientDAO.update(client);
    }
    
    public void deleteAccount(UUID clientId) {
        clientDAO.findById(clientId).ifPresent(clientDAO::delete);
    }
}
