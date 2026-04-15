package com.subtrack.controller;

import com.subtrack.dao.SystemConfigDAO;
import com.subtrack.entity.Client;
import com.subtrack.entity.EmailIntegration;
import com.subtrack.service.EmailIntegrationService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ExternalContext;
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
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;

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
            
            FacesContext ctx = FacesContext.getCurrentInstance();
            
            if (clientId.isBlank() || clientSecret.isBlank()) {
                ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "OAuth not configured. Contact admin."));
                return null;
            }
            
            oauthState = java.util.UUID.randomUUID().toString();
            
            ExternalContext ec = ctx.getExternalContext();
            String baseUrl = ec.getRequestScheme() + "://" + ec.getRequestServerName() 
                + (ec.getRequestServerPort() != 80 && ec.getRequestServerPort() != 443 
                   ? ":" + ec.getRequestServerPort() : "")
                + ec.getRequestContextPath();
            String redirectUri = baseUrl + "/oauth/callback";
            
            String authUrl = "https://accounts.google.com/o/oauth2/v2auth?"
                + "client_id=" + clientId
                + "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, "UTF-8")
                + "&response_type=code"
                + "&scope=" + java.net.URLEncoder.encode("https://www.googleapis.com/auth/gmail.readonly email openid", "UTF-8")
                + "&state=" + oauthState
                + "&access_type=offline"
                + "&prompt=consent";
            
            ec.redirect(authUrl);
            ctx.responseComplete();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
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
            
            ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
            String baseUrl = ec.getRequestScheme() + "://" + ec.getRequestServerName() 
                + (ec.getRequestServerPort() != 80 && ec.getRequestServerPort() != 443 
                   ? ":" + ec.getRequestServerPort() : "")
                + ec.getRequestContextPath();
            String redirectUri = baseUrl + "/oauth/callback";
            
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
                JsonObject jsonObject;
                try (JsonReader reader = Json.createReader(new StringReader(json))) {
                    jsonObject = reader.readObject();
                }
                
                String accessToken = jsonObject.getString("access_token", null);
                String refreshToken = jsonObject.getString("refresh_token", null);
                int expiresIn = jsonObject.getInt("expires_in", 3600);
                
                // Fetch user email address
                String email = fetchUserEmail(accessToken);
                if (email == null) {
                    FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to retrieve email address from Google"));
                    return null;
                }

                Client user = userContext.getCurrentUser();
                if (user != null) {
                    LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiresIn);
                    emailIntegrationService.connectEmail(user, email, accessToken, refreshToken, expiresAt);
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
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "OAuth error: " + e.getMessage()));
            return null;
        }
    }
    
    private String fetchUserEmail(String accessToken) {
        try {
            URL url = new URL("https://www.googleapis.com/oauth2/v3/userinfo");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            
            if (conn.getResponseCode() == 200) {
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }
                
                try (JsonReader reader = Json.createReader(new StringReader(response.toString()))) {
                    JsonObject jsonObject = reader.readObject();
                    return jsonObject.getString("email", null);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch user email: " + e.getMessage());
        }
        return null;
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
