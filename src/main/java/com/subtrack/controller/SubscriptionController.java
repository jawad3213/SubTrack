package com.subtrack.controller;

import com.subtrack.entity.Category;
import com.subtrack.entity.Client;
import com.subtrack.entity.Subscription;
import com.subtrack.enums.Frequency;
import com.subtrack.enums.SubscriptionStatus;
import com.subtrack.service.CategoryService;
import com.subtrack.service.SubscriptionService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Named
@SessionScoped
public class SubscriptionController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SubscriptionService subscriptionService;
    
    @Inject
    private CategoryService categoryService;
    
    @Inject
    private UserContext userContext;

    private List<Subscription> subscriptions;
    private Subscription selectedSubscription;
    private Subscription newSubscription;
    private List<Category> categories;
    private String searchTerm;
    private String editId;
    
    private String name;
    private String description;
    private BigDecimal price;
    private String originalCurrency;
    private Frequency frequency;
    private Category category;
    private LocalDate startDate;
    private String logoUrl;
    private String cancelLink;
    private String notes;

    @PostConstruct
    public void init() {
        loadSubscriptions();
        loadCategories();
        newSubscription = new Subscription();
    }
    
    public void loadSubscriptions() {
        if (userContext.getClientId() != null) {
            subscriptions = subscriptionService.findByClientId(userContext.getClientId());
        } else {
            subscriptions = new ArrayList<>();
        }
    }
    
    public void loadCategories() {
        categories = categoryService.findAll();
    }
    
    public String createSubscription() {
        try {
            if (userContext.getClientId() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "User not logged in"));
                return null;
            }
            
            Client client = userContext.getCurrentUser();
            if (client == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "User session expired"));
                return null;
            }
            
            Subscription subscription = new Subscription();
            subscription.setName(name);
            subscription.setDescription(description);
            subscription.setPrice(price);
            subscription.setOriginalCurrency(originalCurrency != null ? originalCurrency : "USD");
            subscription.setFrequency(frequency != null ? frequency : Frequency.MONTHLY);
            subscription.setCategory(category);
            subscription.setStartDate(startDate != null ? startDate : LocalDate.now());
            subscription.setLogoUrl(logoUrl);
            subscription.setCancelLink(cancelLink);
            subscription.setNotes(notes);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setClient(client);
            
            subscriptionService.create(subscription);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Subscription created successfully"));
            
            clearForm();
            loadSubscriptions();
            return "list?faces-redirect=true";
        } catch (Exception e) {
            e.printStackTrace();
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to create subscription: " + e.getMessage()));
            return null;
        }
    }
    
    public String updateSubscription() {
        try {
            if (selectedSubscription == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No subscription selected"));
                return null;
            }
            
            selectedSubscription.setName(name);
            selectedSubscription.setDescription(description);
            selectedSubscription.setPrice(price);
            selectedSubscription.setOriginalCurrency(originalCurrency);
            selectedSubscription.setFrequency(frequency);
            selectedSubscription.setCategory(category);
            selectedSubscription.setLogoUrl(logoUrl);
            selectedSubscription.setCancelLink(cancelLink);
            selectedSubscription.setNotes(notes);
            
            subscriptionService.update(selectedSubscription);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Subscription updated successfully"));
            
            clearForm();
            loadSubscriptions();
            return "list?faces-redirect=true";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }
    
    public String deleteSubscription() {
        try {
            if (selectedSubscription == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No subscription selected"));
                return null;
            }
            
            subscriptionService.delete(selectedSubscription);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Subscription deleted successfully"));
            
            loadSubscriptions();
            return "/subscriptions/list?faces-redirect=true";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }
    
    public String pauseSubscription() {
        try {
            if (selectedSubscription == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No subscription selected"));
                return null;
            }
            
            subscriptionService.pauseSubscription(selectedSubscription);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Subscription paused"));
            
            loadSubscriptions();
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }
    
    public String cancelSubscription() {
        try {
            if (selectedSubscription == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No subscription selected"));
                return null;
            }
            
            subscriptionService.cancelSubscription(selectedSubscription);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Subscription cancelled"));
            
            loadSubscriptions();
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }
    
    public String reactivateSubscription() {
        try {
            if (selectedSubscription == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "No subscription selected"));
                return null;
            }
            
            subscriptionService.reactivateSubscription(selectedSubscription);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Subscription reactivated"));
            
            loadSubscriptions();
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }
    
    public String prepareEdit(Subscription subscription) {
        selectedSubscription = subscription;
        name = subscription.getName();
        description = subscription.getDescription();
        price = subscription.getPrice();
        originalCurrency = subscription.getOriginalCurrency();
        frequency = subscription.getFrequency();
        category = subscription.getCategory();
        logoUrl = subscription.getLogoUrl();
        cancelLink = subscription.getCancelLink();
        notes = subscription.getNotes();
        return "/subscriptions/edit?faces-redirect=true";
    }
    
    public void loadForEdit() {
        if (editId != null && !editId.trim().isEmpty()) {
            try {
                UUID uuid = UUID.fromString(editId);
                if (selectedSubscription == null || !uuid.equals(selectedSubscription.getId())) {
                    subscriptionService.findById(uuid).ifPresent(sub -> {
                        selectedSubscription = sub;
                        name = sub.getName();
                        description = sub.getDescription();
                        price = sub.getPrice();
                        originalCurrency = sub.getOriginalCurrency();
                        frequency = sub.getFrequency();
                        category = sub.getCategory();
                        logoUrl = sub.getLogoUrl();
                        cancelLink = sub.getCancelLink();
                        notes = sub.getNotes();
                    });
                }
            } catch (IllegalArgumentException e) {
                // Invalid UUID string, ignore and don't load anything
            }
        }
    }
    
    public void prepareNew() {
        clearForm();
    }
    
    private void clearForm() {
        name = null;
        description = null;
        price = null;
        originalCurrency = "USD";
        frequency = Frequency.MONTHLY;
        category = null;
        startDate = LocalDate.now();
        logoUrl = null;
        cancelLink = null;
        notes = null;
        selectedSubscription = null;
    }
    
    public List<Subscription> getActiveSubscriptions() {
        if (subscriptions == null) {
            return new ArrayList<>();
        }
        return subscriptions.stream()
            .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
            .toList();
    }
    
    public BigDecimal getTotalMonthlyCost() {
        if (userContext.getClientId() != null) {
            return subscriptionService.calculateMonthlyCost(userContext.getClientId());
        }
        return BigDecimal.ZERO;
    }
    
    public BigDecimal getTotalAnnualCost() {
        if (userContext.getClientId() != null) {
            return subscriptionService.calculateAnnualCost(userContext.getClientId());
        }
        return BigDecimal.ZERO;
    }

    // Getters and Setters
    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public Subscription getSelectedSubscription() {
        return selectedSubscription;
    }

    public void setSelectedSubscription(Subscription selectedSubscription) {
        this.selectedSubscription = selectedSubscription;
    }

    public Subscription getNewSubscription() {
        return newSubscription;
    }

    public void setNewSubscription(Subscription newSubscription) {
        this.newSubscription = newSubscription;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getEditId() {
        return editId;
    }

    public void setEditId(String editId) {
        this.editId = editId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getOriginalCurrency() {
        return originalCurrency;
    }

    public void setOriginalCurrency(String originalCurrency) {
        this.originalCurrency = originalCurrency;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getCancelLink() {
        return cancelLink;
    }

    public void setCancelLink(String cancelLink) {
        this.cancelLink = cancelLink;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public Frequency[] getFrequencies() {
        return Frequency.values();
    }
    
    public SubscriptionStatus[] getStatuses() {
        return SubscriptionStatus.values();
    }
}
