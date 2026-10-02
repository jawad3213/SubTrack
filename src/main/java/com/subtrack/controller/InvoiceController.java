package com.subtrack.controller;

import com.subtrack.util.FacesErrors;
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

@Named
@SessionScoped
public class InvoiceController implements Serializable {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(InvoiceController.class);

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
        LOGGER.info("InvoiceController: Processing invoice " + (selectedInvoice != null ? selectedInvoice.getId() : "null"));
        if (selectedInvoice != null) {
            try {
                invoiceService.createSubscriptionFromInvoice(selectedInvoice);
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Success", "Subscription created successfully from invoice."));
                LOGGER.info("InvoiceController: Successfully created subscription for invoice " + selectedInvoice.getId());
                loadInvoices();
            } catch (Exception e) {
                LOGGER.warn("InvoiceController: Failed to process invoice", e);
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Error", FacesErrors.message(e, "Failed to process invoice. Please try again.")));
            }
        }
    }

    public void deleteInvoice() {
        LOGGER.info("InvoiceController: Deleting invoice " + (selectedInvoice != null ? selectedInvoice.getId() : "null"));
        if (selectedInvoice != null) {
            try {
                invoiceService.delete(selectedInvoice);
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Success", "Invoice deleted successfully."));
                LOGGER.info("InvoiceController: Successfully deleted invoice " + selectedInvoice.getId());
                loadInvoices();
            } catch (Exception e) {
                LOGGER.warn("InvoiceController: Failed to delete invoice", e);
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Error", FacesErrors.message(e, "Failed to delete invoice. Please try again.")));
            }
        }
    }

    public void updateInvoice() {
        LOGGER.info("InvoiceController: Updating invoice " + (selectedInvoice != null ? selectedInvoice.getId() : "null"));
        if (selectedInvoice != null) {
            try {
                invoiceService.update(selectedInvoice);
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_INFO, 
                    "Success", "Invoice updated successfully."));
                loadInvoices();
            } catch (Exception e) {
                LOGGER.warn("InvoiceController: Failed to update invoice", e);
                FacesContext.getCurrentInstance().addMessage(null, 
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                    "Error", FacesErrors.message(e, "Failed to update invoice. Please try again.")));
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
