package com.subtrack.controller;

import com.subtrack.entity.Client;
import com.subtrack.service.ClientService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.UUID;

@Named
@ViewScoped
public class AdminUserDetailBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ClientService clientService;

    @Inject
    private UserContext userContext;

    private String clientId;
    private Client user;

    public void loadUser() {
        if (clientId != null && !clientId.isEmpty() && userContext.isAdmin()) {
            try {
                UUID id = UUID.fromString(clientId);
                user = clientService.findById(id).orElse(null);
            } catch (Exception e) {
                System.err.println(">>> AdminUserDetailBean loadUser error: " + e.getMessage());
            }
        }
    }

    public String suspendUser() {
        if (user != null) {
            clientService.deactivateClient(user.getId());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Account Suspended", user.getEmail()));
            loadUser();
        }
        return null;
    }

    public String activateUser() {
        if (user != null) {
            clientService.activateClient(user.getId());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Account Activated", user.getEmail()));
            loadUser();
        }
        return null;
    }

    public String deleteUser() {
        if (user != null) {
            clientService.deleteClient(user.getId());
            return "/admin/users?faces-redirect=true";
        }
        return null;
    }

    // Getters and setters
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public Client getUser() { return user; }
}
