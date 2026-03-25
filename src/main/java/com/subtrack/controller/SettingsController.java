package com.subtrack.controller;

import com.subtrack.entity.Client;
import com.subtrack.entity.User;
import com.subtrack.service.ClientService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class SettingsController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ClientService clientService;

    @Inject
    private UserContext userContext;

    private String fullName;
    private String timezone;
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
    private boolean emailNotifications = true;
    private boolean telegramEnabled = false;
    private String telegramChatId;
    private boolean whatsappEnabled = false;
    private String whatsappNumber;

    @PostConstruct
    public void init() {
        loadSettings();
    }

    public void loadSettings() {
        User user = userContext.getCurrentUser();
        if (user instanceof Client client) {
            fullName = client.getFullName();
            timezone = client.getTimezone();
        }
    }

    public String updateProfile() {
        try {
            User user = userContext.getCurrentUser();
            if (user instanceof Client client) {
                String[] nameParts = fullName != null ? fullName.split(" ", 2) : new String[2];
                client.setFirstName(nameParts[0]);
                if (nameParts.length > 1) {
                    client.setLastName(nameParts[1]);
                }
                client.setTimezone(timezone);
                clientService.update(client);
                
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Profile updated successfully"));
            }
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }

    public String changePassword() {
        try {
            if (newPassword == null || !newPassword.equals(confirmPassword)) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Passwords do not match"));
                return null;
            }
            
            if (newPassword.length() < 8) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Password must be at least 8 characters"));
                return null;
            }

            User user = userContext.getCurrentUser();
            if (user instanceof Client client) {
                clientService.changePassword(client.getId(), currentPassword, newPassword);
                
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Password changed successfully"));
                
                currentPassword = null;
                newPassword = null;
                confirmPassword = null;
            }
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }

    public String updateNotificationPreferences() {
        try {
            User user = userContext.getCurrentUser();
            if (user instanceof Client client) {
                clientService.updateNotificationPreferences(
                    client.getId(), 
                    emailNotifications, 
                    telegramEnabled, 
                    telegramChatId,
                    whatsappEnabled,
                    whatsappNumber
                );
                
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Notification preferences updated"));
            }
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }

    public String deleteAccount() {
        try {
            User user = userContext.getCurrentUser();
            if (user instanceof Client client) {
                clientService.deleteAccount(client.getId());
                return "/login.xhtml?faces-redirect=true";
            }
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public boolean isEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public boolean isTelegramEnabled() {
        return telegramEnabled;
    }

    public void setTelegramEnabled(boolean telegramEnabled) {
        this.telegramEnabled = telegramEnabled;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public boolean isWhatsappEnabled() {
        return whatsappEnabled;
    }

    public void setWhatsappEnabled(boolean whatsappEnabled) {
        this.whatsappEnabled = whatsappEnabled;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public void setWhatsappNumber(String whatsappNumber) {
        this.whatsappNumber = whatsappNumber;
    }
}
