package com.subtrack.controller;

import com.subtrack.entity.User;
import com.subtrack.enums.Role;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class UserContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private User currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.getExternalContext().getSessionMap().put("user", user);
        }
    }

    public User getCurrentUser() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            User sessionUser = (User) context.getExternalContext().getSessionMap().get("user");
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
        return getCurrentUser() != null && 
               getCurrentUser().getRole() == Role.ADMIN;
    }

    public String getUserName() {
        if (currentUser != null) {
            return currentUser.getFullName();
        }
        return "Guest";
    }
    
    public java.util.UUID getClientId() {
        if (currentUser != null) {
            return currentUser.getId();
        }
        return null;
    }

    public void invalidate() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.getExternalContext().invalidateSession();
        }
        currentUser = null;
    }
}
