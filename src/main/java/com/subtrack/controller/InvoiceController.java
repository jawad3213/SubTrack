package com.subtrack.controller;

import com.subtrack.entity.Invoice;
import com.subtrack.service.InvoiceService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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
    private boolean showProcessed = false;

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
        if (selectedInvoice != null) {
            invoiceService.processInvoice(selectedInvoice);
            loadInvoices();
        }
    }

    public void deleteInvoice() {
        if (selectedInvoice != null) {
            invoiceService.delete(selectedInvoice);
            loadInvoices();
        }
    }

    public void selectInvoice(Invoice invoice) {
        selectedInvoice = invoice;
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
        if (showProcessed) {
            return invoices;
        }
        return invoices.stream()
            .filter(inv -> !inv.getIsProcessed())
            .toList();
    }

    public Invoice getSelectedInvoice() {
        return selectedInvoice;
    }

    public void setSelectedInvoice(Invoice selectedInvoice) {
        this.selectedInvoice = selectedInvoice;
    }

    public boolean isShowProcessed() {
        return showProcessed;
    }

    public void setShowProcessed(boolean showProcessed) {
        this.showProcessed = showProcessed;
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
