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

    static final int MAX_PARSE_ATTEMPTS = 5;

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(InvoiceService.class);

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
    public void update(Invoice invoice) {
        if (invoice != null) {
            invoiceDAO.update(invoice);
        }
    }
    
    @Transactional
    public Subscription createSubscriptionFromInvoice(Invoice invoice) {
        LOGGER.info("InvoiceService: createSubscriptionFromInvoice called.");
        LOGGER.info("serviceName=" + invoice.getServiceName() + ", amount=" + invoice.getAmount() + ", currency=" + invoice.getCurrency() + ", date=" + invoice.getInvoiceDate() + ", isProcessed=" + invoice.getIsProcessed());

        // Only call the AI if essential fields are truly missing.
        // This avoids burning Gemini quota when data was already parsed during sync.
        boolean needsParsing = (invoice.getServiceName() == null || invoice.getServiceName().isBlank())
                            && invoice.getAmount() == null;

        if (needsParsing && invoice.getRawContent() != null && !invoice.getRawContent().isBlank()) {
            LOGGER.info("InvoiceService: Both serviceName and amount are missing. Attempting AI parsing...");
            try {
                // Call Gemini directly (NOT through @Transactional parseInvoiceEmail)
                // to avoid nested transaction rollback issues
                GeminiService.InvoiceParseResult result = geminiService.extractInvoiceData(invoice.getRawContent());
                if (result.isSuccess()) {
                    if (result.getServiceName() != null) invoice.setServiceName(result.getServiceName());
                    if (result.getAmount() != null) invoice.setAmount(result.getAmount());
                    if (result.getCurrency() != null) invoice.setCurrency(result.getCurrency());
                    if (result.getInvoiceDate() != null) invoice.setInvoiceDate(result.getInvoiceDate());
                    LOGGER.info("InvoiceService: AI parsing succeeded: " + result.getServiceName());
                } else {
                    LOGGER.warn("InvoiceService: AI parsing returned failure: " + result.getErrorMessage());
                }
            } catch (Exception e) {
                LOGGER.warn("InvoiceService: AI parsing failed (quota?), will use fallback values. Error", e);
            }
        }

        // Use whatever data we have — fill in fallbacks for anything still missing
        String name = (invoice.getServiceName() != null && !invoice.getServiceName().isBlank())
                     ? invoice.getServiceName() 
                     : "Unknown Service";
        BigDecimal amount = invoice.getAmount() != null 
                          ? invoice.getAmount() 
                          : BigDecimal.ZERO;
        String currency = (invoice.getCurrency() != null && !invoice.getCurrency().isBlank())
                        ? invoice.getCurrency() 
                        : "USD";
        LocalDate startDate = invoice.getInvoiceDate() != null 
                            ? invoice.getInvoiceDate() 
                            : LocalDate.now();

        Client client = invoice.getClient();
        
        Subscription subscription = new Subscription();
        subscription.setClient(client);
        subscription.setName(name);
        subscription.setPrice(amount);
        subscription.setOriginalCurrency(currency);
        subscription.setFrequency(Frequency.MONTHLY);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(startDate);
        
        subscriptionService.create(subscription);
        LOGGER.info("InvoiceService: Subscription created for '" + name + "' — " + amount + " " + currency);
        
        // Mark invoice as processed
        invoice.setIsProcessed(true);
        invoiceDAO.update(invoice);
        
        return subscription;
    }
    
    /** Unprocessed invoices that haven't used up their AI parsing attempts yet. */
    public List<Invoice> getUnprocessedInvoices() {
        return invoiceDAO.findUnprocessedInvoices(MAX_PARSE_ATTEMPTS);
    }
    
    @Transactional
    public void parseInvoiceEmail(Invoice invoice) {
        if (invoice == null || invoice.getRawContent() == null || invoice.getRawContent().isBlank()) {
            LOGGER.warn("InvoiceService: Cannot parse invoice - no raw content.");
            return;
        }

        GeminiService.InvoiceParseResult result;
        try {
            result = geminiService.extractInvoiceData(invoice.getRawContent());
        } catch (RuntimeException e) {
            LOGGER.warn("Gemini call failed for invoice {}", invoice.getId(), e);
            result = null;
        }

        if (result == null || !result.isSuccess()) {
            // Count the failure so the scheduler stops retrying (and spending Gemini quota) after MAX_PARSE_ATTEMPTS.
            invoice.setParseAttempts(invoice.getParseAttempts() + 1);
            invoiceDAO.update(invoice);
            LOGGER.warn("AI parsing failed for invoice {} (attempt {}/{}): {}", invoice.getId(),
                invoice.getParseAttempts(), MAX_PARSE_ATTEMPTS, result != null ? result.getErrorMessage() : "exception");
            return;
        }

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
        LOGGER.info("InvoiceService: Successfully parsed invoice - " + invoice.getServiceName() + " | " + invoice.getAmount() + " " + invoice.getCurrency());
    }
}
