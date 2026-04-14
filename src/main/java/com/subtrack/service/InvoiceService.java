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

    @Inject
    private GeminiService geminiService;

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
    
    @Transactional
    public void parseInvoiceEmail(Invoice invoice) {
        if (invoice == null || invoice.getRawContent() == null || invoice.getRawContent().isBlank()) {
            System.err.println(">>> InvoiceService: Cannot parse invoice - no raw content.");
            return;
        }

        GeminiService.InvoiceParseResult result = geminiService.extractInvoiceData(invoice.getRawContent());

        if (result.isSuccess()) {
            if (result.getServiceName() != null) {
                invoice.setServiceName(result.getServiceName());
            }
            if (result.getAmount() != null) {
                invoice.setAmount(result.getAmount());
            }
            if (result.getCurrency() != null) {
                invoice.setCurrency(result.getCurrency());
            }
            if (result.getInvoiceDate() != null) {
                invoice.setInvoiceDate(result.getInvoiceDate());
            }
            invoice.setIsProcessed(true);
            invoiceDAO.update(invoice);
            System.out.println(">>> InvoiceService: Successfully parsed invoice - " + invoice.getServiceName() + " | " + invoice.getAmount() + " " + invoice.getCurrency());
        } else {
            System.err.println(">>> InvoiceService: AI parsing failed - " + result.getErrorMessage());
        }
    }
}
