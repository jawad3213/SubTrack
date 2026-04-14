package com.subtrack.service;

import com.subtrack.dao.AlertRuleDAO;
import com.subtrack.dao.NotificationLogDAO;
import com.subtrack.dao.SystemConfigDAO;
import com.subtrack.entity.AlertRule;
import com.subtrack.entity.Client;
import com.subtrack.entity.Subscription;
import com.subtrack.enums.AlertChannel;
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
class AlertServiceTest {

    @Mock
    private AlertRuleDAO alertRuleDAO;

    @Mock
    private EmailService emailService;

    @Mock
    private SystemConfigDAO configDAO;

    @Mock
    private NotificationLogDAO notificationLogDAO;

    @InjectMocks
    private AlertService alertService;

    private Client testClient;
    private Subscription testSubscription;
    private AlertRule testAlert;

    @BeforeEach
    void setUp() {
        testClient = new Client();
        testClient.setId(UUID.randomUUID());
        testClient.setEmail("test@example.com");
        testClient.setFirstName("John");

        testSubscription = new Subscription();
        testSubscription.setId(UUID.randomUUID());
        testSubscription.setClient(testClient);
        testSubscription.setName("Netflix");
        testSubscription.setPrice(new BigDecimal("15.99"));
        testSubscription.setOriginalCurrency("USD");
        testSubscription.setStatus(SubscriptionStatus.ACTIVE);
        testSubscription.setStartDate(LocalDate.now());
        testSubscription.setNextBillingDate(LocalDate.now().plusDays(3));

        testAlert = new AlertRule();
        testAlert.setId(UUID.randomUUID());
        testAlert.setSubscription(testSubscription);
        testAlert.setChannel(AlertChannel.EMAIL);
        testAlert.setTimingDays(3);
        testAlert.setIsActive(true);
    }

    @Test
    void createAlert_Success() {
        doNothing().when(alertRuleDAO).create(any(AlertRule.class));

        alertService.createAlert(testAlert);

        verify(alertRuleDAO).create(any(AlertRule.class));
    }

    @Test
    void createAlertForSubscription_Success() {
        doNothing().when(alertRuleDAO).create(any(AlertRule.class));

        alertService.createAlertForSubscription(testSubscription, AlertChannel.EMAIL, 3);

        verify(alertRuleDAO).create(any(AlertRule.class));
    }

    @Test
    void findAll_ReturnsAlerts() {
        List<AlertRule> alerts = List.of(testAlert);
        when(alertRuleDAO.findAll()).thenReturn(alerts);

        List<AlertRule> result = alertService.findAll();

        assertEquals(1, result.size());
        assertEquals(testAlert.getId(), result.get(0).getId());
    }

    @Test
    void findBySubscriptionId_ReturnsAlerts() {
        List<AlertRule> alerts = List.of(testAlert);
        when(alertRuleDAO.findBySubscriptionId(testSubscription.getId())).thenReturn(alerts);

        List<AlertRule> result = alertService.findBySubscriptionId(testSubscription.getId());

        assertEquals(1, result.size());
    }

    @Test
    void findById_Found() {
        when(alertRuleDAO.findById(testAlert.getId())).thenReturn(Optional.of(testAlert));

        Optional<AlertRule> result = alertService.findById(testAlert.getId());

        assertTrue(result.isPresent());
        assertEquals(testAlert.getId(), result.get().getId());
    }

    @Test
    void findById_NotFound() {
        UUID randomId = UUID.randomUUID();
        when(alertRuleDAO.findById(randomId)).thenReturn(Optional.empty());

        Optional<AlertRule> result = alertRuleDAO.findById(randomId);

        assertFalse(result.isPresent());
    }

    @Test
    void updateAlert_Success() {
        doNothing().when(alertRuleDAO).update(any(AlertRule.class));

        testAlert.setTimingDays(5);
        alertService.updateAlert(testAlert);

        verify(alertRuleDAO).update(any(AlertRule.class));
    }

    @Test
    void deleteAlert_Success() {
        doNothing().when(alertRuleDAO).delete(any(AlertRule.class));

        alertService.deleteAlert(testAlert);

        verify(alertRuleDAO).delete(any(AlertRule.class));
    }

    @Test
    void getActiveAlerts_ReturnsActive() {
        List<AlertRule> activeAlerts = List.of(testAlert);
        when(alertRuleDAO.findActiveAlertRules()).thenReturn(activeAlerts);

        List<AlertRule> result = alertService.getActiveAlerts();

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
    }

    @Test
    void getAlertsDueSoon_ReturnsDueAlerts() {
        List<AlertRule> dueAlerts = List.of(testAlert);
        when(alertRuleDAO.findAlertsDueSoon(3)).thenReturn(dueAlerts);

        List<AlertRule> result = alertService.getAlertsDueSoon(3);

        assertEquals(1, result.size());
    }

    @Test
    void sendEmailNotification_Success() {
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());
        doNothing().when(notificationLogDAO).create(any());

        alertService.sendEmailNotification(testAlert);

        verify(emailService).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendEmailNotification_NoSubscription_DoesNothing() {
        testAlert.setSubscription(null);

        alertService.sendEmailNotification(testAlert);

        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendEmailNotification_NoClient_DoesNothing() {
        testSubscription.setClient(null);

        alertService.sendEmailNotification(testAlert);

        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void createAlertForSubscription_SetsCorrectValues() {
        doNothing().when(alertRuleDAO).create(any(AlertRule.class));

        alertService.createAlertForSubscription(testSubscription, AlertChannel.TELEGRAM, 5);

        verify(alertRuleDAO).create(argThat(alert -> 
            alert.getChannel() == AlertChannel.TELEGRAM &&
            alert.getTimingDays() == 5 &&
            alert.getIsActive() == true
        ));
    }

    @Test
    void createAlertForSubscription_DefaultTimingDays() {
        doNothing().when(alertRuleDAO).create(any(AlertRule.class));

        alertService.createAlertForSubscription(testSubscription, AlertChannel.EMAIL, 0);

        verify(alertRuleDAO).create(argThat(alert -> 
            alert.getTimingDays() == 0
        ));
    }
}