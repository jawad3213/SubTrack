package com.subtrack.controller;

import com.subtrack.util.FacesErrors;
import com.subtrack.dao.SystemConfigDAO;
import com.subtrack.service.AttemptLimiter;
import com.subtrack.service.ClientService;
import com.subtrack.service.EmailService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Named
@RequestScoped
public class ForgotPasswordController implements Serializable {

    private static final long serialVersionUID = 1L;

    // Reset emails allowed per address within the window; extra requests are silently ignored.
    private static final int MAX_RESET_EMAILS = 3;
    private static final Duration RESET_WINDOW = Duration.ofHours(1);

    @Inject
    private AttemptLimiter attemptLimiter;

    @Inject
    private ClientService clientService;

    @Inject
    private EmailService emailService;

    @Inject
    private SystemConfigDAO systemConfigDAO;

    private String email;
    private String resetToken;
    private String newPassword;
    private String confirmPassword;
    private boolean tokenSent;

    public String requestReset() {
        String sanitizedEmail = (email != null) ? email.trim().toLowerCase() : "";
        String limitKey = "reset:email:" + sanitizedEmail;
        Optional<String> token = Optional.empty();
        if (!attemptLimiter.isBlocked(limitKey, MAX_RESET_EMAILS, RESET_WINDOW)) {
            attemptLimiter.record(limitKey);
            token = clientService.createPasswordResetToken(sanitizedEmail);
        }

        if (token.isPresent()) {
            String resetLink = getBaseUrl() + "/reset-password.xhtml?token="
                + URLEncoder.encode(token.get(), StandardCharsets.UTF_8);
            emailService.sendEmail(sanitizedEmail, "SubTrack Password Reset",
                "To reset your password, open the following link:\n" + resetLink +
                "\n\nThis link expires in 1 hour and can only be used once." +
                "\n\nIf you didn't request this, ignore this email; your password has not changed.");
        }

        // Same response whether or not the account exists, so emails can't be enumerated.
        tokenSent = true;
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Check your inbox",
                "If an account exists for this email, a reset link has been sent."));
        return null;
    }

    public boolean isTokenValid() {
        return clientService.isPasswordResetTokenValid(resetToken);
    }

    public String resetPassword() {
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Passwords do not match"));
            return null;
        }

        try {
            clientService.resetPasswordWithToken(resetToken, newPassword);
            FacesContext context = FacesContext.getCurrentInstance();
            context.getExternalContext().getFlash().setKeepMessages(true);
            context.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success",
                    "Password reset successfully. You can now sign in."));
            return "/login.xhtml?faces-redirect=true";
        } catch (IllegalArgumentException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", FacesErrors.message(e)));
            return null;
        }
    }

    /**
     * Prefers the configured APP_BASE_URL so a forged Host header can't redirect reset links
     * to another domain; falls back to the current request for local development.
     */
    private String getBaseUrl() {
        String configured = systemConfigDAO.getValue("APP_BASE_URL", "");
        if (configured != null && !configured.isBlank()) {
            return configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
        }
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        int port = ec.getRequestServerPort();
        return ec.getRequestScheme() + "://" + ec.getRequestServerName()
            + (port != 80 && port != 443 ? ":" + port : "")
            + ec.getRequestContextPath();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getResetToken() { return resetToken; }
    public void setResetToken(String t) { this.resetToken = t; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String p) { this.newPassword = p; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String p) { this.confirmPassword = p; }
    public boolean isTokenSent() { return tokenSent; }
}
