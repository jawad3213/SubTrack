package com.subtrack.entity;

import com.subtrack.enums.Frequency;
import com.subtrack.enums.SubscriptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Subscription} entity.
 * Covers cost calculations, next-billing-date calculation, and edge cases.
 */
class SubscriptionTest {

    private Subscription createSubscription(BigDecimal price, Frequency freq) {
        Subscription s = new Subscription();
        s.setPrice(price);
        s.setFrequency(freq);
        s.setStartDate(LocalDate.now().minusMonths(6));
        s.setStatus(SubscriptionStatus.ACTIVE);
        s.setOriginalCurrency("USD");
        s.setName("TestSub");
        return s;
    }

    // ---------------------------------------------------------------
    // getMonthlyCost
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("getMonthlyCost()")
    class MonthlyCost {

        @Test
        @DisplayName("MONTHLY frequency: monthly cost equals the price")
        void monthly_equalsPrice() {
            Subscription s = createSubscription(new BigDecimal("10.00"), Frequency.MONTHLY);
            assertEquals(0, new BigDecimal("10.00").compareTo(s.getMonthlyCost()));
        }

        @Test
        @DisplayName("ANNUAL frequency: monthly cost = price * (1/12)")
        void annual_dividedBy12() {
            Subscription s = createSubscription(new BigDecimal("120.00"), Frequency.ANNUAL);
            // 120.00 * (1.0/12.0) uses double arithmetic
            BigDecimal expected = new BigDecimal("120.00").multiply(BigDecimal.valueOf(1.0 / 12.0));
            assertEquals(0, expected.compareTo(s.getMonthlyCost()));
        }

        @Test
        @DisplayName("WEEKLY frequency: monthly cost = price * 4.33")
        void weekly_multipliedBy433() {
            Subscription s = createSubscription(new BigDecimal("10.00"), Frequency.WEEKLY);
            assertEquals(0, new BigDecimal("43.30").compareTo(s.getMonthlyCost()));
        }

        @Test
        @DisplayName("QUARTERLY frequency: monthly cost = price * (1/3)")
        void quarterly_dividedBy3() {
            Subscription s = createSubscription(new BigDecimal("30.00"), Frequency.QUARTERLY);
            BigDecimal expected = new BigDecimal("30.00").multiply(BigDecimal.valueOf(1.0 / 3.0));
            assertEquals(0, expected.compareTo(s.getMonthlyCost()));
        }

        @Test
        @DisplayName("null price should return ZERO")
        void nullPrice_returnsZero() {
            Subscription s = createSubscription(null, Frequency.MONTHLY);
            assertEquals(0, BigDecimal.ZERO.compareTo(s.getMonthlyCost()));
        }

        @Test
        @DisplayName("null frequency should return ZERO")
        void nullFrequency_returnsZero() {
            Subscription s = new Subscription();
            s.setPrice(new BigDecimal("10.00"));
            s.setFrequency(null);
            assertEquals(0, BigDecimal.ZERO.compareTo(s.getMonthlyCost()));
        }
    }

    // ---------------------------------------------------------------
    // getAnnualCost
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("getAnnualCost()")
    class AnnualCost {

        @Test
        @DisplayName("MONTHLY frequency: annual cost = price * 12")
        void monthly_times12() {
            Subscription s = createSubscription(new BigDecimal("10.00"), Frequency.MONTHLY);
            assertEquals(0, new BigDecimal("120.00").compareTo(s.getAnnualCost()));
        }

        @Test
        @DisplayName("ANNUAL frequency: annual cost equals the price")
        void annual_equalsPrice() {
            Subscription s = createSubscription(new BigDecimal("99.99"), Frequency.ANNUAL);
            assertEquals(0, new BigDecimal("99.99").compareTo(s.getAnnualCost()));
        }

        @Test
        @DisplayName("WEEKLY frequency: annual cost = price * 52")
        void weekly_times52() {
            Subscription s = createSubscription(new BigDecimal("5.00"), Frequency.WEEKLY);
            assertEquals(0, new BigDecimal("260.00").compareTo(s.getAnnualCost()));
        }

        @Test
        @DisplayName("QUARTERLY frequency: annual cost = price * 4")
        void quarterly_times4() {
            Subscription s = createSubscription(new BigDecimal("30.00"), Frequency.QUARTERLY);
            assertEquals(0, new BigDecimal("120.00").compareTo(s.getAnnualCost()));
        }
    }

    // ---------------------------------------------------------------
    // calculateNextBillingDate
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("calculateNextBillingDate()")
    class NextBilling {

        @Test
        @DisplayName("should set nextBillingDate to a future date")
        void calculate_setsFutureDate() {
            Subscription s = createSubscription(BigDecimal.TEN, Frequency.MONTHLY);
            s.calculateNextBillingDate();

            assertNotNull(s.getNextBillingDate());
            assertTrue(s.getNextBillingDate().isAfter(LocalDate.now()));
        }

        @Test
        @DisplayName("should not change anything when startDate is null")
        void calculate_nullStart_noChange() {
            Subscription s = new Subscription();
            s.setFrequency(Frequency.MONTHLY);
            s.setStartDate(null);

            s.calculateNextBillingDate();

            assertNull(s.getNextBillingDate());
        }
    }

    // ---------------------------------------------------------------
    // Entity helpers
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("Entity helpers")
    class Helpers {

        @Test
        @DisplayName("toString should include name, price, and frequency")
        void toString_includesKeyFields() {
            Subscription s = createSubscription(new BigDecimal("9.99"), Frequency.MONTHLY);
            String str = s.toString();
            assertTrue(str.contains("TestSub"));
            assertTrue(str.contains("9.99"));
            assertTrue(str.contains("Monthly"));
        }

        @Test
        @DisplayName("equals should return true for same id")
        void equals_sameId_isTrue() {
            Subscription s1 = new Subscription();
            Subscription s2 = new Subscription();
            java.util.UUID id = java.util.UUID.randomUUID();
            s1.setId(id);
            s2.setId(id);
            assertEquals(s1, s2);
        }

        @Test
        @DisplayName("equals should return false for different ids")
        void equals_differentIds_isFalse() {
            Subscription s1 = new Subscription();
            Subscription s2 = new Subscription();
            s1.setId(java.util.UUID.randomUUID());
            s2.setId(java.util.UUID.randomUUID());
            assertNotEquals(s1, s2);
        }
    }
}
