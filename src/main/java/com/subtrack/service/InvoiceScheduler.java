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
                invoiceService.processInvoice(invoice.getId());
            } catch (Exception e) {
                System.err.println("Failed to process invoice: " + invoice.getId());
            }
        }
    }
}
