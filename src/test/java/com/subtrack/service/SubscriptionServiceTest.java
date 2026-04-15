package com.subtrack.service;

import com.subtrack.dao.SubscriptionDAO;
import com.subtrack.dao.PaymentHistoryDAO;
import com.subtrack.entity.Client;
import com.subtrack.entity.Subscription;
import com.subtrack.enums.Frequency;
import com.subtrack.enums.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionDAO subscriptionDAO;

    @Mock
    private PaymentHistoryDAO paymentHistoryDAO;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private Client testClient;
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
    }

    @Test
    void create_Success() {
        doNothing().when(subscriptionDAO).create(any(Subscription.class));
        
        subscriptionService.create(testSubscription);
        
        verify(subscriptionDAO).create(any(Subscription.class));
    }

    @Test
    void findByClientId_ReturnsSubscriptions() {
        List<Subscription> subscriptions = List.of(testSubscription);
        when(subscriptionDAO.findByClientId(testClient.getId())).thenReturn(subscriptions);

        List<Subscription> result = subscriptionService.findByClientId(testClient.getId());

        assertEquals(1, result.size());
        assertEquals("Netflix", result.get(0).getName());
    }

    @Test
    void findById_Found() {
        when(subscriptionDAO.findById(testSubscription.getId())).thenReturn(Optional.of(testSubscription));

        Optional<Subscription> result = subscriptionService.findById(testSubscription.getId());

        assertTrue(result.isPresent());
        assertEquals("Netflix", result.get().getName());
    }

    @Test
    void findById_NotFound() {
        UUID randomId = UUID.randomUUID();
        when(subscriptionDAO.findById(randomId)).thenReturn(Optional.empty());

        Optional<Subscription> result = subscriptionService.findById(randomId);

        assertFalse(result.isPresent());
    }

    @Test
    void calculateMonthlyCost_ActiveSubscriptions() {
        List<Subscription> activeSubscriptions = List.of(testSubscription);
        when(subscriptionDAO.findByClientIdAndStatus(testClient.getId(), SubscriptionStatus.ACTIVE))
            .thenReturn(activeSubscriptions);

        BigDecimal result = subscriptionService.calculateMonthlyCost(testClient.getId());

        assertEquals(0, new BigDecimal("15.99").compareTo(result));
    }

    @Test
    void calculateMonthlyCost_MultipleActiveSubscriptions() {
        Subscription sub2 = new Subscription();
        sub2.setId(UUID.randomUUID());
        sub2.setClient(testClient);
        sub2.setName("Spotify");
        sub2.setPrice(new BigDecimal("9.99"));
        sub2.setOriginalCurrency("USD");
        sub2.setFrequency(Frequency.MONTHLY);
        sub2.setStatus(SubscriptionStatus.ACTIVE);

        List<Subscription> subscriptions = List.of(testSubscription, sub2);
        when(subscriptionDAO.findByClientIdAndStatus(testClient.getId(), SubscriptionStatus.ACTIVE))
            .thenReturn(subscriptions);

        BigDecimal result = subscriptionService.calculateMonthlyCost(testClient.getId());

        assertEquals(0, new BigDecimal("25.98").compareTo(result));
    }

    @Test
    void calculateMonthlyCost_ZeroForNoActiveSubscriptions() {
        when(subscriptionDAO.findByClientIdAndStatus(testClient.getId(), SubscriptionStatus.ACTIVE))
            .thenReturn(List.of());

        BigDecimal result = subscriptionService.calculateMonthlyCost(testClient.getId());

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void calculateAnnualCost_MonthlySubscription() {
        List<Subscription> activeSubscriptions = List.of(testSubscription);
        when(subscriptionDAO.findByClientIdAndStatus(testClient.getId(), SubscriptionStatus.ACTIVE))
            .thenReturn(activeSubscriptions);

        BigDecimal result = subscriptionService.calculateAnnualCost(testClient.getId());

        assertEquals(0, new BigDecimal("191.88").compareTo(result));
    }

    @Test
    void calculateAnnualCost_AnnualSubscription() {
        testSubscription.setFrequency(Frequency.ANNUAL);
        testSubscription.setPrice(new BigDecimal("100.00"));
        
        List<Subscription> activeSubscriptions = List.of(testSubscription);
        when(subscriptionDAO.findByClientIdAndStatus(testClient.getId(), SubscriptionStatus.ACTIVE))
            .thenReturn(activeSubscriptions);

        BigDecimal result = subscriptionService.calculateAnnualCost(testClient.getId());

        assertEquals(0, new BigDecimal("100.00").compareTo(result));
    }

    @Test
    void pauseSubscription_Success() {
        when(subscriptionDAO.findById(testSubscription.getId())).thenReturn(Optional.of(testSubscription));
        doNothing().when(subscriptionDAO).update(any(Subscription.class));

        subscriptionService.pauseSubscription(testSubscription);

        assertEquals(SubscriptionStatus.PAUSED, testSubscription.getStatus());
        verify(subscriptionDAO).update(any(Subscription.class));
    }

    @Test
    void cancelSubscription_Success() {
        when(subscriptionDAO.findById(testSubscription.getId())).thenReturn(Optional.of(testSubscription));
        doNothing().when(subscriptionDAO).update(any(Subscription.class));

        subscriptionService.cancelSubscription(testSubscription);

        assertEquals(SubscriptionStatus.CANCELLED, testSubscription.getStatus());
        assertNull(testSubscription.getNextBillingDate());
        verify(subscriptionDAO).update(any(Subscription.class));
    }

    @Test
    void reactivateSubscription_Success() {
        testSubscription.setStatus(SubscriptionStatus.PAUSED);
        when(subscriptionDAO.findById(testSubscription.getId())).thenReturn(Optional.of(testSubscription));
        doNothing().when(subscriptionDAO).update(any(Subscription.class));

        subscriptionService.reactivateSubscription(testSubscription);

        assertEquals(SubscriptionStatus.ACTIVE, testSubscription.getStatus());
        assertNotNull(testSubscription.getNextBillingDate());
        verify(subscriptionDAO).update(any(Subscription.class));
    }

    @Test
    void findActiveByClientId_ReturnsOnlyActive() {
        List<Subscription> activeSubscriptions = List.of(testSubscription);
        when(subscriptionDAO.findByClientIdAndStatus(testClient.getId(), SubscriptionStatus.ACTIVE))
            .thenReturn(activeSubscriptions);

        List<Subscription> result = subscriptionService.findActiveByClientId(testClient.getId());

        assertEquals(1, result.size());
        assertEquals(SubscriptionStatus.ACTIVE, result.get(0).getStatus());
    }

    @Test
    void deleteSubscription_Success() {
        when(subscriptionDAO.findById(testSubscription.getId())).thenReturn(Optional.of(testSubscription));
        doNothing().when(subscriptionDAO).delete(any(Subscription.class));

        subscriptionService.delete(testSubscription);

        verify(subscriptionDAO).delete(any(Subscription.class));
    }

    @Test
    void updateSubscription_Success() {
        doNothing().when(subscriptionDAO).update(any(Subscription.class));

        testSubscription.setName("Netflix Updated");
        subscriptionService.update(testSubscription);

        verify(subscriptionDAO).update(any(Subscription.class));
    }

    @Test
    void calculateMonthlyCost_WeeklySubscription() {
        testSubscription.setFrequency(Frequency.WEEKLY);
        testSubscription.setPrice(new BigDecimal("10.00"));

        List<Subscription> activeSubscriptions = List.of(testSubscription);
        when(subscriptionDAO.findByClientIdAndStatus(testClient.getId(), SubscriptionStatus.ACTIVE))
            .thenReturn(activeSubscriptions);

        BigDecimal result = subscriptionService.calculateMonthlyCost(testClient.getId());

        assertEquals(new BigDecimal("43.30"), result.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void calculateMonthlyCost_QuarterlySubscription() {
        testSubscription.setFrequency(Frequency.QUARTERLY);
        testSubscription.setPrice(new BigDecimal("30.00"));

        List<Subscription> activeSubscriptions = List.of(testSubscription);
        when(subscriptionDAO.findByClientIdAndStatus(testClient.getId(), SubscriptionStatus.ACTIVE))
            .thenReturn(activeSubscriptions);

        BigDecimal result = subscriptionService.calculateMonthlyCost(testClient.getId());

        assertEquals(new BigDecimal("10.00"), result.setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void calculateAnnualCost_WeeklySubscription() {
        testSubscription.setFrequency(Frequency.WEEKLY);
        testSubscription.setPrice(new BigDecimal("10.00"));

        List<Subscription> activeSubscriptions = List.of(testSubscription);
        when(subscriptionDAO.findByClientIdAndStatus(testClient.getId(), SubscriptionStatus.ACTIVE))
            .thenReturn(activeSubscriptions);

        BigDecimal result = subscriptionService.calculateAnnualCost(testClient.getId());

        assertEquals(new BigDecimal("520.00"), result.setScale(2, java.math.RoundingMode.HALF_UP));
    }
}