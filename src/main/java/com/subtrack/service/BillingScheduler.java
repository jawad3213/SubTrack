package com.subtrack.service;

import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Stateless
public class BillingScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BillingScheduler.class);

    @Inject
    private SubscriptionService subscriptionService;

    /**
     * Daily at 00:05, before AlertScheduler runs: past billing dates move to the next cycle,
     * so the dashboard stays correct and renewal alerts keep firing every cycle.
     */
    @Schedule(hour = "0", minute = "5", persistent = false)
    public void rollOverBillingDates() {
        int updated = subscriptionService.rollOverBillingDates(LocalDate.now());
        LOGGER.info("Billing dates rolled forward for {} subscription(s)", updated);
    }
}
