package com.subtrack.controller;

import com.subtrack.entity.Client;
import com.subtrack.entity.SaaSService;
import com.subtrack.enums.Role;
import com.subtrack.service.AdminService;
import com.subtrack.service.SaaSCatalogService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Named
@SessionScoped
public class AdminController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private AdminService adminService;
    
    @Inject
    private SaaSCatalogService catalogService;
    
    @Inject
    private UserContext userContext;

    private List<Client> users;
    private List<SaaSService> catalogServices;
    private Client selectedUser;
    private SaaSService selectedService;
    
    private String serviceName;
    private String serviceDescription;
    private String serviceCategory;
    private String serviceLogoUrl;

    @PostConstruct
    public void init() {
        users = new ArrayList<>();
        catalogServices = new ArrayList<>();
        if (userContext.isAdmin()) {
            loadUsers();
            loadCatalog();
        }
    }
    
    public void loadUsers() {
        users = adminService.getAllUsers();
    }
    
    public void loadCatalog() {
        catalogServices = catalogService.getAllServices();
    }
    
    public String suspendUser() {
        try {
            if (selectedUser != null) {
                adminService.suspendUser(selectedUser.getId());
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "User suspended"));
                loadUsers();
            }
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }
    
    public String activateUser() {
        try {
            if (selectedUser != null) {
                adminService.activateUser(selectedUser.getId());
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "User activated"));
                loadUsers();
            }
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }
    
    public String removeUser() {
        try {
            if (selectedUser != null) {
                adminService.removeUser(selectedUser.getId());
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "User removed"));
                loadUsers();
            }
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }
    
    public String addServiceToCatalog() {
        try {
            catalogService.addServiceToCatalog(serviceName, serviceDescription, serviceLogoUrl, serviceCategory);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Service added to catalog"));
            clearServiceForm();
            loadCatalog();
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }
    
    public String deleteService() {
        try {
            if (selectedService != null) {
                catalogService.deleteService(selectedService.getId());
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Service removed from catalog"));
                loadCatalog();
            }
            return null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
            return null;
        }
    }
    
    private void clearServiceForm() {
        serviceName = null;
        serviceDescription = null;
        serviceCategory = null;
        serviceLogoUrl = null;
    }

    public List<Client> getUsers() {
        return users;
    }

    public void setUsers(List<Client> users) {
        this.users = users;
    }

    public List<SaaSService> getCatalogServices() {
        return catalogServices;
    }

    public void setCatalogServices(List<SaaSService> catalogServices) {
        this.catalogServices = catalogServices;
    }

    public Client getSelectedUser() {
        return selectedUser;
    }

    public void setSelectedUser(Client selectedUser) {
        this.selectedUser = selectedUser;
    }

    public SaaSService getSelectedService() {
        return selectedService;
    }

    public void setSelectedService(SaaSService selectedService) {
        this.selectedService = selectedService;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public String getServiceCategory() {
        return serviceCategory;
    }

    public void setServiceCategory(String serviceCategory) {
        this.serviceCategory = serviceCategory;
    }

    public String getServiceLogoUrl() {
        return serviceLogoUrl;
    }

    public void setServiceLogoUrl(String serviceLogoUrl) {
        this.serviceLogoUrl = serviceLogoUrl;
    }
}
