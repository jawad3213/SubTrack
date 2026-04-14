package com.subtrack.controller;

import com.subtrack.dao.ClientDAO;
import com.subtrack.entity.Client;
import com.subtrack.service.EmailService;
import com.subtrack.service.ClientService;
import com.subtrack.util.PasswordUtil;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Named
@RequestScoped
public class ForgotPasswordController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ClientService clientService;

    @Inject
    private EmailService emailService;

    @Inject
    private ClientDAO clientDAO;

    private String email;
    private String resetToken;
    private boolean tokenSent;
    private boolean tokenVerified;

    public String requestReset() {
        Optional<Client> clientOpt = clientService.findByEmail(email);
        
        if (clientOpt.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                    "No account found with this email address"));
            return null;
        }

        Client client = clientOpt.get();
        String token = UUID.randomUUID().toString();

        client.setPassword(PasswordUtil.hashPassword(token));
        clientDAO.update(client);

        String resetLink = FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath() 
            + "/reset-password.xhtml?token=" + token;

        emailService.sendEmail(email, "SubTrack Password Reset",
            "To reset your password, click the following link:\n" + resetLink + 
            "\n\nThis link will expire in 1 hour.\n\nIf you didn't request this, please ignore this email.");

        tokenSent = true;
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", 
                "Password reset link sent to your email"));
        
        return null;
    }

    public String verifyToken() {
        if (resetToken == null || resetToken.isBlank()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Invalid token"));
            return null;
        }

        try {
            Optional<Client> clientOpt = clientDAO.findAll().stream()
                .filter(c -> clientDAO.findById(c.getId()).isPresent())
                .findFirst();

            if (clientOpt.isPresent()) {
                Client client = clientOpt.get();
                if (client.getUpdatedAt() != null && 
                    java.time.Duration.between(client.getUpdatedAt(), LocalDateTime.now()).toMinutes() < 60) {
                    tokenVerified = true;
                    return "/reset-password.xhtml?faces-redirect=true";
                }
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Token expired or invalid"));
        }
        return null;
    }

    public String resetPassword(String newPassword) {
        Optional<Client> clientOpt = clientService.findByEmail(email);
        
        if (clientOpt.isEmpty()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Session expired"));
            return "/login.xhtml?faces-redirect=true";
        }

        if (!PasswordUtil.isPasswordStrong(newPassword)) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                    "Password must be at least 8 characters with uppercase, lowercase, and digits"));
            return null;
        }

        try {
            clientService.changePassword(clientOpt.get().getId(), "", newPassword);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", 
                    "Password reset successfully"));
            return "/login.xhtml?faces-redirect=true";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", 
                    "Failed to reset password: " + e.getMessage()));
        }
        return null;
    }

    public String getEmail() { return email; }
    public void setEmail(String e) { this.email = e; }
    public String getResetToken() { return resetToken; }
    public void setResetToken(String t) { this.resetToken = t; }
    public boolean isTokenSent() { return tokenSent; }
    public boolean isTokenVerified() { return tokenVerified; }
}