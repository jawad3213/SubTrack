package com.subtrack.controller;

import com.subtrack.entity.Invoice;
import com.subtrack.service.InvoiceService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Named
@SessionScoped
public class InvoiceController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private InvoiceService invoiceService;

    @Inject
    private UserContext userContext;

    private List<Invoice> invoices;
    private Invoice selectedInvoice;
    private String statusFilter = "ALL"; // ALL, PENDING, PROCESSED

    @PostConstruct
    public void init() {
        loadInvoices();
    }

    public void loadInvoices() {
        if (userContext.getClientId() != null) {
            invoices = invoiceService.findByClientId(userContext.getClientId());
        }
    }

    public void processInvoice() {
        System.out.println(">>> InvoiceController: Processing invoice " + (selectedInvoice != null ? selectedInvoice.getId() : "null"));
        if (selectedInvoice != null) {
            try {
                invoiceService.createSubscriptionFromInvoice(selectedInvoice);
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Success", "Subscription created successfully from invoice."));
                System.out.println(">>> InvoiceController: Successfully created subscription for invoice " + selectedInvoice.getId());
                loadInvoices();
            } catch (Exception e) {
                System.err.println(">>> InvoiceController: Failed to process invoice: " + e.getMessage());
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Error", "Failed to process invoice: " + e.getMessage()));
            }
        }
    }

    public void deleteInvoice() {
        System.out.println(">>> InvoiceController: Deleting invoice " + (selectedInvoice != null ? selectedInvoice.getId() : "null"));
        if (selectedInvoice != null) {
            try {
                invoiceService.delete(selectedInvoice);
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Success", "Invoice deleted successfully."));
                System.out.println(">>> InvoiceController: Successfully deleted invoice " + selectedInvoice.getId());
                loadInvoices();
            } catch (Exception e) {
                System.err.println(">>> InvoiceController: Failed to delete invoice: " + e.getMessage());
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Error", "Failed to delete invoice: " + e.getMessage()));
            }
        }
    }

    public void selectInvoice(Invoice invoice) {
        selectedInvoice = invoice;
    }

    public void updateInvoice() {
        System.out.println(">>> InvoiceController: Updating invoice " + (selectedInvoice != null ? selectedInvoice.getId() : "null"));
        if (selectedInvoice != null) {
            try {
                invoiceService.update(selectedInvoice);
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Success", "Invoice updated successfully."));
                loadInvoices();
            } catch (Exception e) {
                System.err.println(">>> InvoiceController: Failed to update invoice: " + e.getMessage());
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Error", "Failed to update invoice: " + e.getMessage()));
            }
        }
    }

    public boolean isConnected() {
        return userContext.getClientId() != null;
    }

    public List<Invoice> getInvoices() {
        if (invoices == null) {
            return List.of();
        }
        return invoices;
    }

    public List<Invoice> getFilteredInvoices() {
        if (invoices == null) {
            return List.of();
        }
        if ("PENDING".equals(statusFilter)) {
            return invoices.stream()
                .filter(inv -> !inv.getIsProcessed())
                .toList();
        } else if ("PROCESSED".equals(statusFilter)) {
            return invoices.stream()
                .filter(inv -> inv.getIsProcessed())
                .toList();
        }
        return invoices; // ALL
    }

    public Invoice getSelectedInvoice() {
        return selectedInvoice;
    }

    public void setSelectedInvoice(Invoice selectedInvoice) {
        this.selectedInvoice = selectedInvoice;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
    }

    public long getPendingCount() {
        if (invoices == null) {
            return 0;
        }
        return invoices.stream()
            .filter(inv -> !inv.getIsProcessed())
            .count();
    }
}
