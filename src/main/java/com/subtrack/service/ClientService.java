package com.subtrack.service;

import com.subtrack.dao.ClientDAO;
import com.subtrack.entity.Client;
import com.subtrack.enums.AccountType;
import com.subtrack.enums.Role;
import com.subtrack.exception.UserFacingException;
import com.subtrack.util.PasswordUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ClientService {

    private static final long PASSWORD_RESET_VALIDITY_MINUTES = 60;

    @Inject
    private ClientDAO clientDAO;

    public Client registerClient(String email, String password, String firstName, 
                                  String lastName, AccountType accountType) {
        if (clientDAO.existsByEmail(email)) {
            throw new UserFacingException("Email already registered");
        }
        
        if (!PasswordUtil.isPasswordStrong(password)) {
            throw new UserFacingException("Password must be at least 8 characters with uppercase, lowercase, and digits");
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

    /**
     * Returns the client if the credentials match. The deactivated-account message is only
     * given after a correct password, so it can't be used to discover which emails exist.
     */
    public Optional<Client> authenticate(String email, String password) {
        Optional<Client> clientOpt = clientDAO.findByEmail(email);
        if (clientOpt.isEmpty() || !PasswordUtil.verifyPassword(password, clientOpt.get().getPassword())) {
            return Optional.empty();
        }
        if (!Boolean.TRUE.equals(clientOpt.get().getIsActive())) {
            throw new UserFacingException("Account is deactivated");
        }
        return clientOpt;
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

    public List<Client> findActiveClients() {
        return clientDAO.findActiveClients();
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
            throw new UserFacingException("Current password is incorrect");
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

    /**
     * Issues a single-use reset token for an active account. The user's current password
     * stays valid until the token is actually used. Returns the raw token to email,
     * or empty if no active account has this email.
     */
    public Optional<String> createPasswordResetToken(String email) {
        Optional<Client> clientOpt = clientDAO.findByEmail(email);
        if (clientOpt.isEmpty() || !Boolean.TRUE.equals(clientOpt.get().getIsActive())) {
            return Optional.empty();
        }
        Client client = clientOpt.get();
        String token = PasswordUtil.generateRandomToken();
        client.setPasswordResetTokenHash(PasswordUtil.sha256Hex(token));
        client.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(PASSWORD_RESET_VALIDITY_MINUTES));
        clientDAO.update(client);
        return Optional.of(token);
    }

    public boolean isPasswordResetTokenValid(String token) {
        return findClientByValidResetToken(token).isPresent();
    }

    public void resetPasswordWithToken(String token, String newPassword) {
        Client client = findClientByValidResetToken(token)
            .orElseThrow(() -> new UserFacingException("This reset link is invalid or has expired"));
        if (!PasswordUtil.isPasswordStrong(newPassword)) {
            throw new UserFacingException("Password must be at least 8 characters with uppercase, lowercase, and digits");
        }
        client.setPassword(PasswordUtil.hashPassword(newPassword));
        client.setPasswordResetTokenHash(null);
        client.setPasswordResetExpiresAt(null);
        clientDAO.update(client);
    }

    private Optional<Client> findClientByValidResetToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return clientDAO.findByPasswordResetTokenHash(PasswordUtil.sha256Hex(token))
            .filter(c -> c.getPasswordResetExpiresAt() != null
                && c.getPasswordResetExpiresAt().isAfter(LocalDateTime.now()));
    }
}
