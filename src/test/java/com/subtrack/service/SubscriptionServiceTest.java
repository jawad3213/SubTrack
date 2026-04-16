package com.subtrack.service;

import com.subtrack.dao.PaymentHistoryDAO;
import com.subtrack.dao.SubscriptionDAO;
import com.subtrack.entity.PaymentHistory;
import com.subtrack.entity.Subscription;
import com.subtrack.enums.Frequency;
import com.subtrack.enums.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SubscriptionService}.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionDAO subscriptionDAO;

    @Mock
    private PaymentHistoryDAO paymentHistoryDAO;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private final UUID clientId = UUID.randomUUID();

    private Subscription createSub(String name, BigDecimal price, Frequency freq, SubscriptionStatus status) {
        Subscription s = new Subscription();
        s.setId(UUID.randomUUID());
        s.setName(name);
        s.setPrice(price);
        s.setFrequency(freq);
        s.setStatus(status);
        s.setStartDate(LocalDate.now().minusMonths(3));
        s.setOriginalCurrency("USD");
        return s;
    }

    // ---------------------------------------------------------------
    // calculateMonthlyCost
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("calculateMonthlyCost()")
    class CalculateMonthlyCost {

        @Test
        @DisplayName("should sum monthly costs of active subscriptions")
        void calculate_multipleActive_sumsCorrectly() {
            Subscription s1 = createSub("Netflix", new BigDecimal("15.99"), Frequency.MONTHLY, SubscriptionStatus.ACTIVE);
            Subscription s2 = createSub("Spotify", new BigDecimal("9.99"), Frequency.MONTHLY, SubscriptionStatus.ACTIVE);
            when(subscriptionDAO.findByClientIdAndStatus(clientId, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(s1, s2));

            BigDecimal result = subscriptionService.calculateMonthlyCost(clientId);

            // 15.99 * 1.0 + 9.99 * 1.0 = 25.98
            assertEquals(0, new BigDecimal("25.98").compareTo(result));
        }

        @Test
        @DisplayName("should return zero when there are no active subscriptions")
        void calculate_noActive_returnsZero() {
            when(subscriptionDAO.findByClientIdAndStatus(clientId, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of());

            assertEquals(0, BigDecimal.ZERO.compareTo(subscriptionService.calculateMonthlyCost(clientId)));
        }

        @Test
        @DisplayName("should convert annual subscription to monthly cost correctly")
        void calculate_annualFrequency_dividesBy12() {
            Subscription annual = createSub("Domain", new BigDecimal("120.00"), Frequency.ANNUAL, SubscriptionStatus.ACTIVE);
            when(subscriptionDAO.findByClientIdAndStatus(clientId, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(annual));

            BigDecimal result = subscriptionService.calculateMonthlyCost(clientId);

            // 120.00 * (1.0/12.0) uses double arithmetic via Frequency.getMonthlyMultiplier()
            BigDecimal expected = new BigDecimal("120.00").multiply(BigDecimal.valueOf(1.0 / 12.0));
            assertEquals(0, expected.compareTo(result));
        }
    }

    // ---------------------------------------------------------------
    // calculateAnnualCost
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("calculateAnnualCost()")
    class CalculateAnnualCost {

        @Test
        @DisplayName("should multiply monthly subscription by 12")
        void annual_monthlyFreq_multipliesBy12() {
            Subscription monthly = createSub("Netflix", new BigDecimal("15.99"), Frequency.MONTHLY, SubscriptionStatus.ACTIVE);
            when(subscriptionDAO.findByClientIdAndStatus(clientId, SubscriptionStatus.ACTIVE))
                    .thenReturn(List.of(monthly));

            BigDecimal result = subscriptionService.calculateAnnualCost(clientId);

            // 15.99 * 12 = 191.88
            assertEquals(0, new BigDecimal("191.88").compareTo(result));
        }
    }

    // ---------------------------------------------------------------
    // pauseSubscription / cancelSubscription / reactivateSubscription
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Status transitions")
    class StatusTransitions {

        @Test
        @DisplayName("pauseSubscription should set status to PAUSED")
        void pause_setsStatusPaused() {
            Subscription sub = createSub("Test", BigDecimal.TEN, Frequency.MONTHLY, SubscriptionStatus.ACTIVE);

            subscriptionService.pauseSubscription(sub);

            assertEquals(SubscriptionStatus.PAUSED, sub.getStatus());
            verify(subscriptionDAO).update(sub);
        }

        @Test
        @DisplayName("cancelSubscription should set status to CANCELLED and clear next billing")
        void cancel_setsStatusCancelled() {
            Subscription sub = createSub("Test", BigDecimal.TEN, Frequency.MONTHLY, SubscriptionStatus.ACTIVE);
            sub.setNextBillingDate(LocalDate.now().plusDays(10));

            subscriptionService.cancelSubscription(sub);

            assertEquals(SubscriptionStatus.CANCELLED, sub.getStatus());
            assertNull(sub.getNextBillingDate());
            verify(subscriptionDAO).update(sub);
        }

        @Test
        @DisplayName("reactivateSubscription should set status to ACTIVE")
        void reactivate_setsStatusActive() {
            Subscription sub = createSub("Test", BigDecimal.TEN, Frequency.MONTHLY, SubscriptionStatus.PAUSED);

            subscriptionService.reactivateSubscription(sub);

            assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
            verify(subscriptionDAO).update(sub);
        }
    }

    // ---------------------------------------------------------------
    // detectRedundantSubscriptions
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("detectRedundantSubscriptions()")
    class DetectRedundant {

        @Test
        @DisplayName("should detect duplicate subscriptions with same name")
        void detect_duplicates_returnsSuggestion() {
            Subscription s1 = createSub("Netflix", new BigDecimal("15.99"), Frequency.MONTHLY, SubscriptionStatus.ACTIVE);
            Subscription s2 = createSub("Netflix", new BigDecimal("9.99"), Frequency.MONTHLY, SubscriptionStatus.ACTIVE);
            when(subscriptionDAO.findByClientId(clientId)).thenReturn(List.of(s1, s2));

            var suggestions = subscriptionService.detectRedundantSubscriptions(clientId);

            assertEquals(1, suggestions.size());
            assertTrue(suggestions.get(0).getTitle().contains("Duplicate"));
            // Potential savings = the 2nd subscription's monthly cost
            assertEquals(0, new BigDecimal("9.99").compareTo(suggestions.get(0).getPotentialSavings()));
        }

        @Test
        @DisplayName("should return empty when there are no duplicates")
        void detect_noDuplicates_returnsEmpty() {
            Subscription s1 = createSub("Netflix", BigDecimal.TEN, Frequency.MONTHLY, SubscriptionStatus.ACTIVE);
            Subscription s2 = createSub("Spotify", BigDecimal.TEN, Frequency.MONTHLY, SubscriptionStatus.ACTIVE);
            when(subscriptionDAO.findByClientId(clientId)).thenReturn(List.of(s1, s2));

            var suggestions = subscriptionService.detectRedundantSubscriptions(clientId);

            assertTrue(suggestions.isEmpty());
        }
    }

    // ---------------------------------------------------------------
    // detectUnusedSubscriptions
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("detectUnusedSubscriptions()")
    class DetectUnused {

        @Test
        @DisplayName("should flag active subscriptions with no payment history")
        void detect_noPayments_returnsSuggestion() {
            Subscription sub = createSub("Unused Service", new BigDecimal("5.00"), Frequency.MONTHLY, SubscriptionStatus.ACTIVE);
            when(subscriptionDAO.findByClientId(clientId)).thenReturn(List.of(sub));
            when(paymentHistoryDAO.findBySubscriptionId(sub.getId())).thenReturn(List.of());

            var suggestions = subscriptionService.detectUnusedSubscriptions(clientId);

            assertEquals(1, suggestions.size());
            assertTrue(suggestions.get(0).getTitle().contains("Unused"));
        }

        @Test
        @DisplayName("should not flag subscriptions with multiple payments")
        void detect_hasPayments_returnsEmpty() {
            Subscription sub = createSub("Active Service", new BigDecimal("10.00"), Frequency.MONTHLY, SubscriptionStatus.ACTIVE);
            PaymentHistory p1 = new PaymentHistory();
            PaymentHistory p2 = new PaymentHistory();
            when(subscriptionDAO.findByClientId(clientId)).thenReturn(List.of(sub));
            when(paymentHistoryDAO.findBySubscriptionId(sub.getId())).thenReturn(List.of(p1, p2));

            var suggestions = subscriptionService.detectUnusedSubscriptions(clientId);

            assertTrue(suggestions.isEmpty());
        }
    }

    // ---------------------------------------------------------------
    // CRUD delegations
    // ---------------------------------------------------------------
    @Test
    @DisplayName("findById should delegate to DAO")
    void findById_delegates() {
        UUID id = UUID.randomUUID();
        Subscription sub = createSub("X", BigDecimal.ONE, Frequency.MONTHLY, SubscriptionStatus.ACTIVE);
        when(subscriptionDAO.findById(id)).thenReturn(Optional.of(sub));

        Optional<Subscription> result = subscriptionService.findById(id);

        assertTrue(result.isPresent());
        verify(subscriptionDAO).findById(id);
    }

    @Test
    @DisplayName("create should delegate to DAO")
    void create_delegates() {
        Subscription sub = createSub("New", BigDecimal.TEN, Frequency.MONTHLY, SubscriptionStatus.ACTIVE);
        subscriptionService.create(sub);
        verify(subscriptionDAO).create(sub);
    }

    @Test
    @DisplayName("delete should delegate to DAO")
    void delete_delegates() {
        Subscription sub = createSub("Old", BigDecimal.ONE, Frequency.MONTHLY, SubscriptionStatus.CANCELLED);
        subscriptionService.delete(sub);
        verify(subscriptionDAO).delete(sub);
    }
}