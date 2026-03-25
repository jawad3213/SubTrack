package com.subtrack.controller;

import com.subtrack.entity.SaaSService;
import com.subtrack.service.SaaSCatalogService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class AdminCatalogBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SaaSCatalogService catalogService;

    @Inject
    private UserContext userContext;

    private List<SaaSService> catalogServices = new ArrayList<>();
    private List<Object> categories = new ArrayList<>();
    private SaaSService selectedService;
    private String serviceName;
    private String serviceLogoUrl;
    private String serviceCategory;
    private String servicePrice;
    private String serviceCurrency = "MAD";
    private String cancelUrl;

    @PostConstruct
    public void init() {
        if (userContext.isAdmin()) {
            catalogServices = catalogService.getAllServices();
            categories = java.util.Arrays.asList(
                "Entertainment", "Productivity", "Cloud Storage", "Software Development", 
                "Design", "Marketing", "E-commerce", "Hosting", "Other"
            );
        }
    }

    public String addService() {
        try {
            SaaSService service = new SaaSService();
            service.setName(serviceName);
            service.setCategory(serviceCategory);
            service.setLogoUrl(serviceLogoUrl);
            service.setWebsiteUrl(cancelUrl);
            
            if (servicePrice != null && !servicePrice.isEmpty()) {
                service.setDefaultPrice(new java.math.BigDecimal(servicePrice));
                service.setDefaultCurrency(serviceCurrency);
            }
            
            catalogService.createService(service);
            
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Service added to catalog"));
            clearForm();
            catalogServices = catalogService.getAllServices();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
        return null;
    }

    public String deleteService() {
        if (selectedService != null) {
            try {
                catalogService.deleteService(selectedService.getId());
                catalogServices = catalogService.getAllServices();
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            }
        }
        return null;
    }

    private void clearForm() {
        serviceName = null;
        serviceLogoUrl = null;
        serviceCategory = null;
        servicePrice = null;
        cancelUrl = null;
    }

    // Getters and setters
    public List<SaaSService> getCatalogServices() { return catalogServices; }
    public List<Object> getCategories() { return categories; }
    public SaaSService getSelectedService() { return selectedService; }
    public void setSelectedService(SaaSService s) { this.selectedService = s; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String s) { this.serviceName = s; }
    public String getServiceLogoUrl() { return serviceLogoUrl; }
    public void setServiceLogoUrl(String s) { this.serviceLogoUrl = s; }
    public String getServiceCategory() { return serviceCategory; }
    public void setServiceCategory(String s) { this.serviceCategory = s; }
    public String getServicePrice() { return servicePrice; }
    public void setServicePrice(String s) { this.servicePrice = s; }
    public String getServiceCurrency() { return serviceCurrency; }
    public void setServiceCurrency(String s) { this.serviceCurrency = s; }
    public String getCancelUrl() { return cancelUrl; }
    public void setCancelUrl(String s) { this.cancelUrl = s; }
}
