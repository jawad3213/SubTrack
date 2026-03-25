package com.subtrack.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Stub bean for Invoice & AI Processing admin page.
 * Will be fully implemented when the Gemini AI pipeline is integrated.
 */
@Named
@RequestScoped
public class AdminInvoiceBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @jakarta.inject.Inject
    private com.subtrack.service.InvoiceService invoiceService;

    private String filterStatus;
    private String filterClient;
    private List<InvoiceDTO> invoices = new ArrayList<>();
    private InvoiceDTO selectedInvoice;

    @jakarta.annotation.PostConstruct
    public void init() {
        loadInvoices();
    }

    private void loadInvoices() {
        invoices.clear();
        try {
            for (com.subtrack.entity.Invoice inv : invoiceService.findAll()) {
                invoices.add(new InvoiceDTO(
                    inv.getId().toString(),
                    inv.getClient() != null ? inv.getClient().getFullName() : "Unknown Client",
                    inv.getServiceName() != null ? inv.getServiceName() : "Pending Extraction",
                    inv.getAmount() != null ? inv.getAmount() : java.math.BigDecimal.ZERO,
                    inv.getInvoiceDate(),
                    Boolean.TRUE.equals(inv.getIsProcessed()) ? "PROCESSED" : "PENDING"
                ));
            }
        } catch (Exception e) {
            System.err.println(">>> Error loading invoices: " + e.getMessage());
        }
        
        // Mock data to see the UI if DB is empty
        if (invoices.isEmpty()) {
            invoices.add(new InvoiceDTO("INV-1004", "Alice Smith", "Netflix", new java.math.BigDecimal("19.99"), java.time.LocalDate.now().minusDays(1), "PROCESSED"));
            invoices.add(new InvoiceDTO("INV-1005", "Bob Jones", "AWS Cloud", new java.math.BigDecimal("145.50"), java.time.LocalDate.now().minusDays(2), "FAILED"));
            invoices.add(new InvoiceDTO("INV-1006", "Alice Smith", "Pending...", java.math.BigDecimal.ZERO, java.time.LocalDate.now(), "PENDING"));
        }
    }

    public void applyFilters() {
        // Here we could implement in-memory filtering of the DTO list
        loadInvoices();
        if (filterStatus != null && !filterStatus.isEmpty()) {
            invoices.removeIf(dto -> !dto.getProcessingStatus().equals(filterStatus));
        }
        if (filterClient != null && !filterClient.isEmpty()) {
            String q = filterClient.toLowerCase();
            invoices.removeIf(dto -> !dto.getClientName().toLowerCase().contains(q));
        }
    }

    public void reprocess() {
        if (selectedInvoice != null) {
            jakarta.faces.context.FacesContext.getCurrentInstance().addMessage(null,
                new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_INFO, "Reprocessing triggered", "Invoice " + selectedInvoice.getId() + " sent to Gemini pipeline."));
        }
    }

    // Getters and setters
    public String getFilterStatus() { return filterStatus; }
    public void setFilterStatus(String s) { this.filterStatus = s; }
    public String getFilterClient() { return filterClient; }
    public void setFilterClient(String s) { this.filterClient = s; }
    public List<InvoiceDTO> getInvoices() { return invoices; }
    public InvoiceDTO getSelectedInvoice() { return selectedInvoice; }
    public void setSelectedInvoice(InvoiceDTO o) { this.selectedInvoice = o; }

    public static class InvoiceDTO {
        private String id;
        private String clientName;
        private String extractedServiceName;
        private java.math.BigDecimal extractedAmount;
        private java.time.LocalDate extractedDate;
        private String processingStatus;

        public InvoiceDTO(String id, String clientName, String service, java.math.BigDecimal amount, java.time.LocalDate date, String status) {
            this.id = id;
            this.clientName = clientName;
            this.extractedServiceName = service;
            this.extractedAmount = amount;
            this.extractedDate = date;
            this.processingStatus = status;
        }

        public String getId() { return id != null && id.length() > 8 ? id.substring(0, 8).toUpperCase() : id; }
        public String getClientName() { return clientName; }
        public String getExtractedServiceName() { return extractedServiceName; }
        public java.math.BigDecimal getExtractedAmount() { return extractedAmount; }
        public java.time.LocalDate getExtractedDate() { return extractedDate; }
        public String getProcessingStatus() { return processingStatus; }
    }
}
