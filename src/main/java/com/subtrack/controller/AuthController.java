package com.subtrack.controller;

import com.subtrack.util.FacesErrors;
import com.subtrack.entity.Client;
import com.subtrack.enums.AccountType;
import com.subtrack.enums.Role;
import com.subtrack.service.AttemptLimiter;
import com.subtrack.service.ClientService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.time.Duration;

/**
 * Handles the login/register forms. Request-scoped so typed passwords are not kept in the
 * session; who is logged in lives only in {@link UserContext}.
 */
@Named
@RequestScoped
public class AuthController implements Serializable {

    private static final long serialVersionUID = 1L;

    // Failed logins allowed per email / per IP address within the window before login is refused.
    private static final int MAX_FAILURES_PER_EMAIL = 5;
    private static final int MAX_FAILURES_PER_IP = 20;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);

    @Inject
    private ClientService clientService;

    @Inject
    private AttemptLimiter attemptLimiter;
    
    @Inject
    private UserContext userContext;

    private String email;
    private String password;
    private String confirmPassword;
    private String firstName;
    private String lastName;
    private AccountType accountType;

    public String login() {
        String sanitizedEmail = (email != null) ? email.trim().toLowerCase() : "";
        String emailKey = "login:email:" + sanitizedEmail;
        String ipKey = "login:ip:" + clientIp();

        if (attemptLimiter.isBlocked(emailKey, MAX_FAILURES_PER_EMAIL, FAILURE_WINDOW)
                || attemptLimiter.isBlocked(ipKey, MAX_FAILURES_PER_IP, FAILURE_WINDOW)) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Login Failed", "Too many failed attempts. Please wait 15 minutes and try again."));
            return null;
        }

        try {
            var result = clientService.authenticate(sanitizedEmail, password);
            if (result.isPresent()) {
                attemptLimiter.reset(emailKey);
                Client loggedInUser = result.get();
                renewSessionId();
                userContext.setCurrentUser(loggedInUser);
                if (loggedInUser.getRole() == Role.ADMIN) {
                    return "/admin/dashboard?faces-redirect=true";
                }
                return "dashboard?faces-redirect=true";
            } else {
                attemptLimiter.record(emailKey);
                attemptLimiter.record(ipKey);
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Login Failed", "Invalid email or password"));
                return null;
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Error", FacesErrors.message(e)));
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
            
            renewSessionId();
            userContext.setCurrentUser(client);
            
            return "dashboard?faces-redirect=true";
        } catch (IllegalArgumentException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Registration Failed", FacesErrors.message(e)));
            return null;
        }
    }

    private String clientIp() {
        return ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).getRemoteAddr();
    }

    /** Prevents session fixation: the pre-login session ID must not stay valid after authentication. */
    private void renewSessionId() {
        ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).changeSessionId();
    }

    public String logout() {
        userContext.invalidate();
        return "/login?faces-redirect=true";
    }

    public boolean isLoggedIn() {
        return userContext.isLoggedIn();
    }

    public boolean isAdmin() {
        return userContext.isAdmin();
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

}
