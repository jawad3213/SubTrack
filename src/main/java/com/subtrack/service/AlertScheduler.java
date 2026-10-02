package com.subtrack.service;

import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class AlertScheduler {

    @Inject
    private AlertService alertService;

    /** Daily at 08:00 server time. Non-persistent so a restart doesn't replay missed runs. */
    @Schedule(hour = "8", minute = "0", persistent = false)
    public void sendDueAlerts() {
        alertService.checkAndSendAlerts();
    }
}
