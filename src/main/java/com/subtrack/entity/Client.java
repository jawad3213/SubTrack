package com.subtrack.entity;

import com.subtrack.enums.AccountType;
import com.subtrack.enums.Role;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "client")
// primary key is inherited from User
public class Client extends User {

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType = AccountType.B2C;

    @Column(length = 50)
    private String timezone;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subscription> subscriptions = new ArrayList<>();

    @OneToOne(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private EmailIntegration emailIntegration;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Invoice> invoices = new ArrayList<>();
    
    @Column(name = "email_notifications")
    private Boolean emailNotifications = true;
    
    @Column(name = "telegram_enabled")
    private Boolean telegramEnabled = false;
    
    @Column(name = "telegram_chat_id", length = 50)
    private String telegramChatId;
    
    @Column(name = "whatsapp_enabled")
    private Boolean whatsappEnabled = false;
    
    @Column(name = "whatsapp_number", length = 20)
    private String whatsappNumber;

    public Client() {
        this.setRole(Role.CLIENT);
    }

    // Getters and Setters
    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public EmailIntegration getEmailIntegration() {
        return emailIntegration;
    }

    public void setEmailIntegration(EmailIntegration emailIntegration) {
        this.emailIntegration = emailIntegration;
    }

    public List<Invoice> getInvoices() {
        return invoices;
    }

    public void setInvoices(List<Invoice> invoices) {
        this.invoices = invoices;
    }
    
    public Boolean getEmailNotifications() {
        return emailNotifications;
    }
    
    public void setEmailNotifications(Boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }
    
    public Boolean getTelegramEnabled() {
        return telegramEnabled;
    }
    
    public void setTelegramEnabled(Boolean telegramEnabled) {
        this.telegramEnabled = telegramEnabled;
    }
    
    public String getTelegramChatId() {
        return telegramChatId;
    }
    
    public void setTelegramChatId(String telegramChatId) {
        this.telegramChatId = telegramChatId;
    }
    
    public Boolean getWhatsappEnabled() {
        return whatsappEnabled;
    }
    
    public void setWhatsappEnabled(Boolean whatsappEnabled) {
        this.whatsappEnabled = whatsappEnabled;
    }
    
    public String getWhatsappNumber() {
        return whatsappNumber;
    }
    
    public void setWhatsappNumber(String whatsappNumber) {
        this.whatsappNumber = whatsappNumber;
    }
}
