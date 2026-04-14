package com.subtrack.controller;

import com.subtrack.dao.SystemConfigDAO;
import com.subtrack.entity.Client;
import com.subtrack.entity.EmailIntegration;
import com.subtrack.service.EmailIntegrationService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    
    @Inject
    private SystemConfigDAO systemConfigDAO;

    private EmailIntegration emailIntegration;
    private String emailAddress;
    private boolean isConnected;
    private String oauthState;

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
    
    public String initiateOAuth() {
        try {
            String clientId = systemConfigDAO.getValue("GOOGLE_OAUTH_CLIENT_ID", "");
            String clientSecret = systemConfigDAO.getValue("GOOGLE_OAUTH_CLIENT_SECRET", "");
            
            if (clientId.isBlank() || clientSecret.isBlank()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "OAuth not configured. Contact admin."));
                return null;
            }
            
            oauthState = java.util.UUID.randomUUID().toString();
            
            FacesContext ctx = FacesContext.getCurrentInstance();
            String baseUrl = ctx.getExternalContext().getRequestContextPath();
            String redirectUri = baseUrl + "/oauth/callback.xhtml";
            
            String authUrl = "https://accounts.google.com/o/oauth2/v2auth?"
                + "client_id=" + clientId
                + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, "UTF-8")
                + "&response_type=code"
                + "&scope=" + java.net.URLEncoder.encode("https://www.googleapis.com/auth/gmail.readonly", "UTF-8")
                + "&state=" + oauthState
                + "&access_type=offline"
                + "&prompt=consent";
            
            ctx.getExternalContext().redirect(authUrl);
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to initiate OAuth: " + e.getMessage()));
            return null;
        }
    }
    
    public String handleOAuthCallback(String code, String state) {
        try {
            if (code == null || code.isBlank()) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Authorization failed"));
                return null;
            }
            
            String clientId = systemConfigDAO.getValue("GOOGLE_OAUTH_CLIENT_ID", "");
            String clientSecret = systemConfigDAO.getValue("GOOGLE_OAUTH_CLIENT_SECRET", "");
            
            FacesContext ctx = FacesContext.getCurrentInstance();
            String baseUrl = ctx.getExternalContext().getRequestContextPath();
            String redirectUri = baseUrl + "/oauth/callback.xhtml";
            
            URL url = new URL("https://oauth2.googleapis.com/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            
            String params = "client_id=" + clientId
                + "&client_secret=" + clientSecret
                + "&code=" + code
                + "&grant_type=authorization_code"
                + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, "UTF-8");
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(params.getBytes(StandardCharsets.UTF_8));
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }
                
                String json = response.toString();
                String accessToken = extractJsonField(json, "access_token");
                String refreshToken = extractJsonField(json, "refresh_token");
                Integer expiresIn = parseInt(extractJsonField(json, "expires_in"));
                
                Client user = userContext.getCurrentUser();
                if (user != null) {
                    LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresIn != null ? expiresIn : 3600);
                    emailIntegrationService.connectEmail(user, emailAddress, accessToken, refreshToken, expiresAt);
                }
                
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Email connected successfully"));
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Token exchange failed"));
            }
            
            loadEmailIntegration();
            return "/settings.xhtml?faces-redirect=true";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }
    
    private String extractJsonField(String json, String field) {
        int idx = json.indexOf("\"" + field + "\"");
        if (idx == -1) return null;
        int colon = json.indexOf(":", idx);
        if (colon == -1) return null;
        int start = json.indexOf("\"", colon);
        if (start == -1) return null;
        int end = json.indexOf("\"", start + 1);
        if (end == -1) return null;
        return json.substring(start + 1, end);
    }
    
    private Integer parseInt(String s) {
        try { return s != null ? Integer.parseInt(s) : null; } catch (Exception e) { return null; }
    }
    
    public String connectEmail() {
        Client user = userContext.getCurrentUser();
        if (user == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "User not logged in"));
            return null;
        }
        return initiateOAuth();
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
    
    public String getOauthState() {
        return oauthState;
    }
}
