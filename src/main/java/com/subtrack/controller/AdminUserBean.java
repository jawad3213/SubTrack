package com.subtrack.controller;

import com.subtrack.entity.Client;
import com.subtrack.service.ClientService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@Named
@ViewScoped
public class AdminUserBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private ClientService clientService;

    @Inject
    private UserContext userContext;

    private List<Client> users;
    private Client selectedUser;
    private String searchQuery;
    private String filterAccountType;
    private String filterStatus;

    @PostConstruct
    public void init() {
        if (userContext.isAdmin()) {
            loadUsers();
        }
    }

    private void loadUsers() {
        try {
            List<Client> all = clientService.findAll();
            if (all != null) {
                users = all.stream()
                    .filter(u -> {
                        if (searchQuery != null && !searchQuery.isEmpty()) {
                            String q = searchQuery.toLowerCase();
                            String name = u.getFullName() != null ? u.getFullName().toLowerCase() : "";
                            String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                            return name.contains(q) || email.contains(q);
                        }
                        return true;
                    })
                    .filter(u -> {
                        if (filterAccountType != null && !filterAccountType.isEmpty()) {
                            return u.getAccountType() != null && u.getAccountType().name().equals(filterAccountType);
                        }
                        return true;
                    })
                    .filter(u -> {
                        if (filterStatus != null && !filterStatus.isEmpty()) {
                            boolean isActive = "true".equals(filterStatus);
                            return u.getIsActive() != null && u.getIsActive() == isActive;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println(">>> AdminUserBean loadUsers error: " + e.getMessage());
        }
    }

    public void search() {
        loadUsers();
    }

    public void suspendUser() {
        if (selectedUser != null) {
            clientService.deactivateClient(selectedUser.getId());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "User Suspended", selectedUser.getEmail()));
            loadUsers();
        }
    }

    public void activateUser() {
        if (selectedUser != null) {
            clientService.activateClient(selectedUser.getId());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "User Activated", selectedUser.getEmail()));
            loadUsers();
        }
    }

    public void deleteUser() {
        if (selectedUser != null) {
            clientService.deleteClient(selectedUser.getId());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "User Deleted", selectedUser.getEmail()));
            loadUsers();
        }
    }

    // Getters and setters
    public List<Client> getUsers() { return users; }
    public Client getSelectedUser() { return selectedUser; }
    public void setSelectedUser(Client selectedUser) { this.selectedUser = selectedUser; }
    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
    public String getFilterAccountType() { return filterAccountType; }
    public void setFilterAccountType(String filterAccountType) { this.filterAccountType = filterAccountType; }
    public String getFilterStatus() { return filterStatus; }
    public void setFilterStatus(String filterStatus) { this.filterStatus = filterStatus; }
}
