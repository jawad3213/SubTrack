package com.subtrack.service;

import com.subtrack.dao.InvoiceDAO;
import com.subtrack.entity.Client;
import com.subtrack.entity.Invoice;
import com.subtrack.entity.Subscription;
import com.subtrack.enums.Frequency;
import com.subtrack.enums.SubscriptionStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class InvoiceService {

    @Inject
    private InvoiceDAO invoiceDAO;
    
    @Inject
    private SubscriptionService subscriptionService;

    public List<Invoice> findAll() {
        return invoiceDAO.findAll();
    }

    public List<Invoice> findByClientId(UUID clientId) {
        return invoiceDAO.findByClientId(clientId);
    }

    public Optional<Invoice> findById(UUID id) {
        return invoiceDAO.findById(id);
    }

    @Transactional
    public Invoice createInvoice(Client client, String rawContent, String serviceName, 
                                  BigDecimal amount, String currency, LocalDate invoiceDate) {
        Invoice invoice = new Invoice();
        invoice.setClient(client);
        invoice.setRawContent(rawContent);
        invoice.setServiceName(serviceName);
        invoice.setAmount(amount);
        invoice.setCurrency(currency != null ? currency : "USD");
        invoice.setInvoiceDate(invoiceDate != null ? invoiceDate : LocalDate.now());
        invoice.setIsProcessed(false);
        
        invoiceDAO.create(invoice);
        return invoice;
    }

    @Transactional
    public void processInvoice(UUID invoiceId) {
        Optional<Invoice> invoiceOpt = invoiceDAO.findById(invoiceId);
        if (invoiceOpt.isPresent()) {
            Invoice invoice = invoiceOpt.get();
            invoice.setIsProcessed(true);
            invoiceDAO.update(invoice);
        }
    }
    
    @Transactional
    public void processInvoice(Invoice invoice) {
        if (invoice != null) {
            invoice.setIsProcessed(true);
            invoiceDAO.update(invoice);
        }
    }
    
    @Transactional
    public void delete(Invoice invoice) {
        if (invoice != null) {
            invoiceDAO.delete(invoice);
        }
    }
    
    @Transactional
    public Subscription createSubscriptionFromInvoice(Invoice invoice) {
        Client client = invoice.getClient();
        
        Subscription subscription = new Subscription();
        subscription.setClient(client);
        subscription.setName(invoice.getServiceName());
        subscription.setPrice(invoice.getAmount());
        subscription.setOriginalCurrency(invoice.getCurrency());
        subscription.setFrequency(Frequency.MONTHLY);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(invoice.getInvoiceDate());
        
        subscriptionService.create(subscription);
        
        processInvoice(invoice.getId());
        
        return subscription;
    }
    
    public List<Invoice> getUnprocessedInvoices() {
        return invoiceDAO.findUnprocessedInvoices();
    }
    
    public void parseInvoiceEmail(Invoice invoice) {
        
    }
}
