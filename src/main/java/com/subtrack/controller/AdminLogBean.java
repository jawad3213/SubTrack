package com.subtrack.controller;

import com.subtrack.dao.SystemLogDAO;
import com.subtrack.entity.SystemLog;
import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class AdminLogBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SystemLogDAO systemLogDAO;

    private String filterLevel;
    private String searchKeyword;
    private List<SystemLog> logs = new ArrayList<>();

    @PostConstruct
    public void init() {
        applyFilters();
    }

    public void applyFilters() {
        logs.clear();
        if ((filterLevel == null || filterLevel.isBlank()) && 
            (searchKeyword == null || searchKeyword.isBlank())) {
            logs = systemLogDAO.findAll();
        } else if (filterLevel != null && !filterLevel.isBlank() && 
                   searchKeyword != null && !searchKeyword.isBlank()) {
            logs = systemLogDAO.findByLevelAndKeyword(filterLevel, searchKeyword);
        } else if (filterLevel != null && !filterLevel.isBlank()) {
            logs = systemLogDAO.findByLevel(filterLevel);
        } else if (searchKeyword != null && !searchKeyword.isBlank()) {
            logs = systemLogDAO.findByKeyword(searchKeyword);
        }
    }

    public void exportCsv() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            context.getExternalContext().redirect("export_system_logs.xhtml");
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Export failed: " + e.getMessage()));
        }
    }

    public void clearOldLogs() {
        try {
            int deleted = systemLogDAO.deleteOldLogs(30);
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Deleted " + deleted + " old log entries."));
            applyFilters();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Failed to clear logs: " + e.getMessage()));
        }
    }

    public String getFilterLevel() { return filterLevel; }
    public void setFilterLevel(String s) { this.filterLevel = s; }
    public String getSearchKeyword() { return searchKeyword; }
    public void setSearchKeyword(String s) { this.searchKeyword = s; }
    public List<SystemLog> getLogs() { return logs; }
}
