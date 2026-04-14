package com.subtrack.service;

import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;
import com.subtrack.entity.Invoice;

@Stateless
public class InvoiceScheduler {

    @Inject
    private InvoiceService invoiceService;
    
    @Schedule(minute = "*/5", hour = "*")
    public void processPendingInvoices() {
        List<Invoice> unprocessed = invoiceService.getUnprocessedInvoices();
        for (Invoice invoice : unprocessed) {
            try {
                if (invoice.getRawContent() != null && !invoice.getRawContent().isBlank()) {
                    invoiceService.parseInvoiceEmail(invoice);
                    
                    if (invoice.getIsProcessed() && invoice.getServiceName() != null 
                        && invoice.getAmount() != null && invoice.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                        invoiceService.createSubscriptionFromInvoice(invoice);
                    }
                } else {
                    invoiceService.processInvoice(invoice.getId());
                }
            } catch (Exception e) {
                System.err.println("Failed to process invoice: " + invoice.getId() + " - " + e.getMessage());
            }
        }
    }
}
