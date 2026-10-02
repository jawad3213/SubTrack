package com.subtrack.controller;

import com.subtrack.entity.Category;
import com.subtrack.entity.Subscription;
import com.subtrack.enums.Frequency;
import com.subtrack.enums.SubscriptionStatus;
import com.subtrack.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private UserContext userContext;

    @InjectMocks
    private DashboardController controller;

    private final UUID clientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(userContext.getClientId()).thenReturn(clientId);
        when(subscriptionService.getDisplayCurrency()).thenReturn("MAD");
        // Amounts are already in the display currency for these tests.
        lenient().when(subscriptionService.monthlyCostInDisplayCurrency(any()))
            .thenAnswer(inv -> ((Subscription) inv.getArgument(0)).getMonthlyCost());
    }

    private static Subscription sub(String name, String price, SubscriptionStatus status, Category category, LocalDate nextBilling) {
        Subscription s = new Subscription();
        s.setId(UUID.randomUUID());
        s.setName(name);
        s.setPrice(new BigDecimal(price));
        s.setFrequency(Frequency.MONTHLY);
        s.setStatus(status);
        s.setCategory(category);
        s.setStartDate(LocalDate.now().minusMonths(2));
        s.setNextBillingDate(nextBilling);
        return s;
    }

    private static Category category(String name, String color) {
        Category c = new Category();
        c.setName(name);
        c.setColor(color);
        return c;
    }

    @Test
    void computesTotalsBreakdownAndRenewals() {
        Category work = category("Work", "#007bff");
        Subscription a = sub("Slack", "75.00", SubscriptionStatus.ACTIVE, work, LocalDate.now().plusDays(2));
        Subscription b = sub("Netflix", "25.00", SubscriptionStatus.ACTIVE, null, LocalDate.now().plusDays(40));
        Subscription c = sub("Old", "10.00", SubscriptionStatus.CANCELLED, null, null);
        when(subscriptionService.findByClientId(clientId)).thenReturn(List.of(a, b, c));

        controller.init();

        assertEquals(0, new BigDecimal("100.00").compareTo(controller.getTotalMonthlyCost()));
        assertEquals(0, new BigDecimal("1200.00").compareTo(controller.getTotalAnnualCost()));
        assertEquals(2, controller.getActiveSubscriptionCount());
        assertEquals(1, controller.getCancelledCount());

        // Biggest category first, uncategorized gets a fallback color, percentages add up
        assertEquals("Work", controller.getCategoryBreakdown().get(0).getCategoryName());
        assertEquals(75, controller.getCategoryBreakdown().get(0).getPercent());
        assertEquals("Uncategorized", controller.getCategoryBreakdown().get(1).getCategoryName());
        assertNotNull(controller.getCategoryBreakdown().get(1).getColor());
        assertEquals("conic-gradient(#007bff 0% 75%, #639922 75% 100%)", controller.getCategoryDonutGradient());

        // Only Slack renews within 30 days
        assertEquals(1, controller.getUpcomingRenewals().size());
        assertEquals("Slack", controller.getNextRenewal().getSubscription().getName());
        assertTrue(controller.getNextRenewal().isSoon());

        assertEquals("Slack", controller.getTopSubscriptions().get(0).getSubscription().getName());
        assertEquals(100, controller.getTopSubscriptions().get(0).getPercentOfMax());
        assertEquals(6, controller.getTrend().size());
    }

    @Test
    void emptyAccountShowsNothing() {
        when(subscriptionService.findByClientId(clientId)).thenReturn(List.of());

        controller.init();

        assertFalse(controller.isHasSubscriptions());
        assertEquals(0, BigDecimal.ZERO.compareTo(controller.getTotalMonthlyCost()));
        assertEquals("conic-gradient(#E6F1FB 0 100%)", controller.getCategoryDonutGradient());
        assertNull(controller.getNextRenewal());
    }
}
