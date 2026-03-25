package com.subtrack.controller;

import com.subtrack.entity.Client;
import com.subtrack.entity.EmailIntegration;
import com.subtrack.service.EmailIntegrationService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

@Named
@SessionScoped
public class EmailController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private EmailIntegrationService emailIntegrationService;
    
    @Inject
    private UserContext userContext;

    private EmailIntegration emailIntegration;
    private String emailAddress;
    private boolean isConnected;

    @PostConstruct
    public void init() {
        loadEmailIntegration();
    }
    
    public void loadEmailIntegration() {
        if (userContext.getClientId() != null) {
            Optional<EmailIntegration> integration = emailIntegrationService.findByClientId(userContext.getClientId());
            if (integration.isPresent()) {
                emailIntegration = integration.get();
                isConnected = emailIntegration.getIsActive() != null && emailIntegration.getIsActive() && !emailIntegration.isTokenExpired();
            } else {
                isConnected = false;
            }
        }
    }
    
    public String connectEmail() {
        try {
            Client user = userContext.getCurrentUser();
            if (user == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "User not logged in"));
                return null;
            }
            
            emailIntegrationService.connectEmail(user, emailAddress, "mock_access_token", 
                "mock_refresh_token", LocalDateTime.now().plusHours(1));
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Email connected successfully"));
            
            loadEmailIntegration();
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }
    
    public String disconnectEmail() {
        try {
            if (userContext.getClientId() != null) {
                emailIntegrationService.disconnectEmail(userContext.getClientId());
                
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Email disconnected"));
                
                loadEmailIntegration();
            }
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }
    
    public boolean isTokenValid() {
        if (userContext.getClientId() != null) {
            return emailIntegrationService.isTokenValid(userContext.getClientId());
        }
        return false;
    }

    public EmailIntegration getEmailIntegration() {
        return emailIntegration;
    }

    public void setEmailIntegration(EmailIntegration emailIntegration) {
        this.emailIntegration = emailIntegration;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }
}
