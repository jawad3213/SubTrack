package com.subtrack.controller;

import com.subtrack.entity.Client;
import com.subtrack.entity.User;
import com.subtrack.enums.AccountType;
import com.subtrack.enums.Role;
import com.subtrack.service.ClientService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class AuthController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ClientService clientService;
    
    @Inject
    private UserContext userContext;

    private String email;
    private String password;
    private String confirmPassword;
    private String firstName;
    private String lastName;
    private AccountType accountType;
    private User loggedInUser;
    private boolean loggedIn = false;

    public String login() {
        System.out.println(">>> AuthController.login called for email: " + email);
        try {
            String sanitizedEmail = (email != null) ? email.trim().toLowerCase() : null;
            var result = clientService.authenticate(sanitizedEmail, password);
            System.out.println(">>> Authenticate result isPresent: " + result.isPresent());
            if (result.isPresent()) {
                loggedInUser = result.get();
                loggedIn = true;
                userContext.setCurrentUser(loggedInUser);
                System.out.println(">>> Login successful, redirecting...");
                if (loggedInUser.getRole() == Role.ADMIN) {
                    return "/admin/dashboard?faces-redirect=true";
                }
                return "dashboard?faces-redirect=true";
            } else {
                System.out.println(">>> Login failed, no matches");
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Login Failed", "Invalid email or password"));
                return null;
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Error", e.getMessage()));
            return null;
        }
    }

    public String register() {
        try {
            if (!password.equals(confirmPassword)) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Registration Failed", "Passwords do not match"));
                return null;
            }

            String sanitizedEmail = (email != null) ? email.trim().toLowerCase() : null;
            Client client = clientService.registerClient(sanitizedEmail, password, 
                firstName, lastName, accountType);
            
            loggedInUser = client;
            loggedIn = true;
            userContext.setCurrentUser(loggedInUser);
            
            return "dashboard?faces-redirect=true";
        } catch (IllegalArgumentException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Registration Failed", e.getMessage()));
            return null;
        }
    }

    public String logout() {
        userContext.invalidate();
        loggedInUser = null;
        loggedIn = false;
        return "login?faces-redirect=true";
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public boolean isAdmin() {
        return loggedInUser != null && 
               loggedInUser.getRole() == Role.ADMIN;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(User loggedInUser) {
        this.loggedInUser = loggedInUser;
    }
}
