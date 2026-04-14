package com.subtrack.service;

import com.subtrack.dao.ExchangeRateDAO;
import com.subtrack.dao.PaymentHistoryDAO;
import com.subtrack.dao.SubscriptionDAO;
import com.subtrack.entity.Category;
import com.subtrack.entity.ExchangeRate;
import com.subtrack.entity.PaymentHistory;
import com.subtrack.entity.Subscription;
import com.subtrack.enums.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.subtrack.entity.Client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateDAO exchangeRateDAO;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private PaymentHistoryDAO paymentHistoryDAO;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private ExchangeRate eurRate;
    private ExchangeRate gbpRate;
    private Client testClient;
    private Subscription testSubscription;

    @BeforeEach
    void setUp() {
        testClient = new Client();
        testClient.setId(UUID.randomUUID());

        testSubscription = new Subscription();
        testSubscription.setId(UUID.randomUUID());
        testSubscription.setClient(testClient);
        testSubscription.setName("Netflix");
        testSubscription.setPrice(new BigDecimal("15.99"));
        testSubscription.setOriginalCurrency("USD");
        testSubscription.setFrequency(com.subtrack.enums.Frequency.MONTHLY);
        testSubscription.setStatus(SubscriptionStatus.ACTIVE);
        
        eurRate = new ExchangeRate();
        eurRate.setId(UUID.randomUUID());
        eurRate.setFromCurrency("USD");
        eurRate.setToCurrency("EUR");
        eurRate.setRate(new BigDecimal("0.85"));

        gbpRate = new ExchangeRate();
        gbpRate.setId(UUID.randomUUID());
        gbpRate.setFromCurrency("USD");
        gbpRate.setToCurrency("GBP");
        gbpRate.setRate(new BigDecimal("0.73"));
    }

    @Test
    void convertToLocalCurrency_SameCurrency_ReturnsOriginal() {
        BigDecimal result = exchangeRateService.convertToLocalCurrency(
            new BigDecimal("100.00"), "USD", "USD");

        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void convertToLocalCurrency_DirectRate() {
        when(exchangeRateDAO.findByCurrencies("USD", "EUR")).thenReturn(Optional.of(eurRate));

        BigDecimal result = exchangeRateService.convertToLocalCurrency(
            new BigDecimal("100.00"), "USD", "EUR");

        assertEquals(new BigDecimal("85.00"), result);
    }

    @Test
    void convertToLocalCurrency_InverseRate_Available() {
        ExchangeRate inverseRate = new ExchangeRate();
        inverseRate.setFromCurrency("EUR");
        inverseRate.setToCurrency("USD");
        inverseRate.setRate(new BigDecimal("1.18"));

        when(exchangeRateDAO.findByCurrencies("USD", "EUR")).thenReturn(Optional.empty());
        when(exchangeRateDAO.findByCurrencies("EUR", "USD")).thenReturn(Optional.of(inverseRate));

        BigDecimal result = exchangeRateService.convertToLocalCurrency(
            new BigDecimal("100.00"), "USD", "EUR");

        assertNotNull(result);
    }

    @Test
    void convertToLocalCurrency_NullAmount_ReturnsNull() {
        BigDecimal result = exchangeRateService.convertToLocalCurrency(
            null, "USD", "EUR");

        assertNull(result);
    }

    @Test
    void convertToLocalCurrency_NullCurrencies_ReturnsAmount() {
        BigDecimal result = exchangeRateService.convertToLocalCurrency(
            new BigDecimal("100.00"), null, "EUR");

        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void convertToLocalCurrency_NoRateFound_ReturnsOriginal() {
        when(exchangeRateDAO.findByCurrencies("USD", "JPY")).thenReturn(Optional.empty());
        when(exchangeRateDAO.findByCurrencies("JPY", "USD")).thenReturn(Optional.empty());

        BigDecimal result = exchangeRateService.convertToLocalCurrency(
            new BigDecimal("100.00"), "USD", "JPY");

        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void save_NewRate_Created() {
        doNothing().when(exchangeRateDAO).create(any(ExchangeRate.class));

        exchangeRateService.save(eurRate);

        verify(exchangeRateDAO).create(any(ExchangeRate.class));
    }

    @Test
    void save_ExistingRate_Updated() {
        when(exchangeRateDAO.findByCurrencies("USD", "EUR")).thenReturn(Optional.of(eurRate));
        doNothing().when(exchangeRateDAO).update(any(ExchangeRate.class));

        ExchangeRate newRate = new ExchangeRate();
        newRate.setFromCurrency("USD");
        newRate.setToCurrency("EUR");
        newRate.setRate(new BigDecimal("0.90"));

        exchangeRateService.save(newRate);

        verify(exchangeRateDAO).update(any(ExchangeRate.class));
    }

    @Test
    void findAll_ReturnsRates() {
        List<ExchangeRate> rates = List.of(eurRate, gbpRate);
        when(exchangeRateDAO.findAll()).thenReturn(rates);

        List<ExchangeRate> result = exchangeRateService.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void findByCurrencies_Found() {
        when(exchangeRateDAO.findByCurrencies("USD", "EUR")).thenReturn(Optional.of(eurRate));

        Optional<ExchangeRate> result = exchangeRateService.findByCurrencies("USD", "EUR");

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("0.85"), result.get().getRate());
    }

    @Test
    void findByCurrencies_NotFound() {
        when(exchangeRateDAO.findByCurrencies("USD", "JPY")).thenReturn(Optional.empty());

        Optional<ExchangeRate> result = exchangeRateService.findByCurrencies("USD", "JPY");

        assertFalse(result.isPresent());
    }

    @Test
    void convertToLocalCurrency_GBP_DirectRate() {
        when(exchangeRateDAO.findByCurrencies("USD", "GBP")).thenReturn(Optional.of(gbpRate));

        BigDecimal result = exchangeRateService.convertToLocalCurrency(
            new BigDecimal("100.00"), "USD", "GBP");

        assertEquals(new BigDecimal("73.00"), result);
    }

    @Test
    void convertToLocalCurrency_WithDecimalPrecision() {
        when(exchangeRateDAO.findByCurrencies("USD", "EUR")).thenReturn(Optional.of(eurRate));

        BigDecimal result = exchangeRateService.convertToLocalCurrency(
            new BigDecimal("33.33"), "USD", "EUR");

        assertNotNull(result);
    }

    @Test
    void save_ZeroRate() {
        ExchangeRate zeroRate = new ExchangeRate();
        zeroRate.setFromCurrency("USD");
        zeroRate.setToCurrency("XXX");
        zeroRate.setRate(BigDecimal.ZERO);

        doNothing().when(exchangeRateDAO).create(any(ExchangeRate.class));

        exchangeRateService.save(zeroRate);

        verify(exchangeRateDAO).create(any(ExchangeRate.class));
    }

    @Test
    void convertToLocalCurrency_HandlesNegativeAmount() {
        when(exchangeRateDAO.findByCurrencies("USD", "EUR")).thenReturn(Optional.of(eurRate));

        BigDecimal result = exchangeRateService.convertToLocalCurrency(
            new BigDecimal("-50.00"), "USD", "EUR");

        assertTrue(result.compareTo(BigDecimal.ZERO) < 0);
    }
}