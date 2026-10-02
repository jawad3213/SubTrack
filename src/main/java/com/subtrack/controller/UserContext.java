package com.subtrack.controller;

import com.subtrack.entity.Client;
import com.subtrack.enums.Role;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private Client currentUser;

    public void setCurrentUser(Client user) {
        this.currentUser = user;
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.getExternalContext().getSessionMap().put("user", user);
        }
    }

    public Client getCurrentUser() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            Client sessionUser = (Client) context.getExternalContext().getSessionMap().get("user");
            if (sessionUser != null) {
                currentUser = sessionUser;
            }
        }
        return currentUser;
    }

    public boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    public boolean isAdmin() {
        Client user = getCurrentUser();
        return user != null && user.getRole() == Role.ADMIN;
    }

    public String getUserName() {
        Client user = getCurrentUser();
        return user != null ? user.getFullName() : "Guest";
    }
    
    public java.util.UUID getClientId() {
        Client user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    public void invalidate() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.getExternalContext().invalidateSession();
        }
        currentUser = null;
    }
}
