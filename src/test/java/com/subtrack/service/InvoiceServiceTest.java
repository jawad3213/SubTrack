package com.subtrack.service;

import com.subtrack.dao.InvoiceDAO;
import com.subtrack.entity.Client;
import com.subtrack.entity.Invoice;
import com.subtrack.entity.Subscription;
import com.subtrack.enums.Frequency;
import com.subtrack.enums.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceDAO invoiceDAO;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private InvoiceService invoiceService;

    private Client testClient;
    private Invoice testInvoice;
    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        testClient = new Client();
        testClient.setId(UUID.randomUUID());
        testClient.setEmail("test@example.com");

        testSubscription = new Subscription();
        testSubscription.setId(UUID.randomUUID());
        testSubscription.setClient(testClient);
        testSubscription.setName("Netflix");
        testSubscription.setPrice(new BigDecimal("15.99"));
        testSubscription.setOriginalCurrency("USD");
        testSubscription.setFrequency(Frequency.MONTHLY);
        testSubscription.setStatus(SubscriptionStatus.ACTIVE);
        testSubscription.setStartDate(LocalDate.now());

        testInvoice = new Invoice();
        testInvoice.setId(UUID.randomUUID());
        testInvoice.setClient(testClient);
        testInvoice.setServiceName("Netflix");
        testInvoice.setAmount(new BigDecimal("15.99"));
        testInvoice.setCurrency("USD");
        testInvoice.setInvoiceDate(LocalDate.now());
        testInvoice.setIsProcessed(false);
    }

    @Test
    void createInvoice_Success() {
        doNothing().when(invoiceDAO).create(any(Invoice.class));

        Invoice result = invoiceService.createInvoice(
            testClient, "raw content", "Netflix", 
            new BigDecimal("15.99"), "USD", LocalDate.now());

        assertNotNull(result);
        assertEquals("Netflix", result.getServiceName());
        assertEquals(new BigDecimal("15.99"), result.getAmount());
        assertEquals("USD", result.getCurrency());
        assertFalse(result.getIsProcessed());
    }

    @Test
    void createInvoice_DefaultCurrency() {
        doNothing().when(invoiceDAO).create(any(Invoice.class));

        Invoice result = invoiceService.createInvoice(
            testClient, "raw content", "Netflix", 
            new BigDecimal("15.99"), null, LocalDate.now());

        assertEquals("USD", result.getCurrency());
    }

    @Test
    void findAll_ReturnsInvoices() {
        List<Invoice> invoices = List.of(testInvoice);
        when(invoiceDAO.findAll()).thenReturn(invoices);

        List<Invoice> result = invoiceService.findAll();

        assertEquals(1, result.size());
    }

    @Test
    void findByClientId_ReturnsClientInvoices() {
        List<Invoice> invoices = List.of(testInvoice);
        when(invoiceDAO.findByClientId(testClient.getId())).thenReturn(invoices);

        List<Invoice> result = invoiceService.findByClientId(testClient.getId());

        assertEquals(1, result.size());
    }

    @Test
    void findById_Found() {
        when(invoiceDAO.findById(testInvoice.getId())).thenReturn(Optional.of(testInvoice));

        Optional<Invoice> result = invoiceService.findById(testInvoice.getId());

        assertTrue(result.isPresent());
        assertEquals("Netflix", result.get().getServiceName());
    }

    @Test
    void findById_NotFound() {
        UUID randomId = UUID.randomUUID();
        when(invoiceDAO.findById(randomId)).thenReturn(Optional.empty());

        Optional<Invoice> result = invoiceService.findById(randomId);

        assertFalse(result.isPresent());
    }

    @Test
    void processInvoice_MarksAsProcessed() {
        when(invoiceDAO.findById(testInvoice.getId())).thenReturn(Optional.of(testInvoice));
        doNothing().when(invoiceDAO).update(any(Invoice.class));

        invoiceService.processInvoice(testInvoice.getId());

        assertTrue(testInvoice.getIsProcessed());
        verify(invoiceDAO).update(any(Invoice.class));
    }

    @Test
    void processInvoice_NullInvoice_DoesNothing() {
        invoiceService.processInvoice((Invoice) null);
        
        verify(invoiceDAO, never()).update(any());
    }

    @Test
    void createSubscriptionFromInvoice_Success() {
        when(invoiceDAO.findById(testInvoice.getId())).thenReturn(Optional.of(testInvoice));
        doNothing().when(invoiceDAO).update(any(Invoice.class));
        doNothing().when(subscriptionService).create(any(Subscription.class));

        Subscription result = invoiceService.createSubscriptionFromInvoice(testInvoice);

        assertNotNull(result);
        assertEquals("Netflix", result.getName());
        assertEquals(new BigDecimal("15.99"), result.getPrice());
        assertTrue(testInvoice.getIsProcessed());
    }

    @Test
    void createSubscriptionFromInvoice_SetsDefaultValues() {
        testInvoice.setServiceName("Spotify");
        testInvoice.setAmount(new BigDecimal("9.99"));
        testInvoice.setCurrency("EUR");
        testInvoice.setInvoiceDate(LocalDate.of(2024, 1, 15));
        
        when(invoiceDAO.findById(testInvoice.getId())).thenReturn(Optional.of(testInvoice));
        doNothing().when(invoiceDAO).update(any(Invoice.class));
        doNothing().when(subscriptionService).create(any(Subscription.class));

        Subscription result = invoiceService.createSubscriptionFromInvoice(testInvoice);

        assertEquals(Frequency.MONTHLY, result.getFrequency());
        assertEquals(SubscriptionStatus.ACTIVE, result.getStatus());
        assertEquals(testInvoice.getInvoiceDate(), result.getStartDate());
    }

    @Test
    void getUnprocessedInvoices_ReturnsUnprocessed() {
        List<Invoice> unprocessed = List.of(testInvoice);
        when(invoiceDAO.findUnprocessedInvoices()).thenReturn(unprocessed);

        List<Invoice> result = invoiceService.getUnprocessedInvoices();

        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsProcessed());
    }

    @Test
    void deleteInvoice_Success() {
        doNothing().when(invoiceDAO).delete(any(Invoice.class));

        invoiceService.delete(testInvoice);

        verify(invoiceDAO).delete(any(Invoice.class));
    }

    @Test
    void deleteInvoice_Null_DoesNothing() {
        invoiceService.delete(null);

        verify(invoiceDAO, never()).delete(any());
    }

    @Test
    void createInvoice_NullContent_UsesTodaysDate() {
        doNothing().when(invoiceDAO).create(any(Invoice.class));

        Invoice result = invoiceService.createInvoice(
            testClient, null, "Netflix", 
            new BigDecimal("15.99"), "USD", null);

        assertNotNull(result.getInvoiceDate());
    }
}