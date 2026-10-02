package com.subtrack.service;

import com.subtrack.dao.ExchangeRateDAO;
import com.subtrack.dao.SystemConfigDAO;
import com.subtrack.entity.ExchangeRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CurrencyService}.
 */
@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {

    @Mock
    private ExchangeRateDAO exchangeRateDAO;

    @Mock
    private SystemConfigDAO systemConfigDAO;

    @InjectMocks
    private CurrencyService currencyService;

    private static ExchangeRate rate(String from, String to, String value) {
        ExchangeRate r = new ExchangeRate();
        r.setFromCurrency(from);
        r.setToCurrency(to);
        r.setRate(new BigDecimal(value));
        return r;
    }

    @Test
    @DisplayName("same currency returns the amount unchanged")
    void sameCurrency_returnsOriginal() {
        BigDecimal amount = new BigDecimal("100.00");
        assertEquals(0, amount.compareTo(currencyService.convert(amount, "USD", "USD")));
        verifyNoInteractions(exchangeRateDAO);
    }

    @Test
    @DisplayName("null currencies return the amount unchanged")
    void nullCurrency_returnsOriginal() {
        BigDecimal amount = new BigDecimal("50.00");
        assertEquals(0, amount.compareTo(currencyService.convert(amount, null, "EUR")));
        assertEquals(0, amount.compareTo(currencyService.convert(amount, "USD", null)));
    }

    @Test
    @DisplayName("null amount converts to zero")
    void nullAmount_isZero() {
        assertEquals(0, BigDecimal.ZERO.compareTo(currencyService.convert(null, "USD", "EUR")));
    }

    @Test
    @DisplayName("direct rate multiplies")
    void directRate_multiplies() {
        when(exchangeRateDAO.findByCurrencies("USD", "MAD")).thenReturn(Optional.of(rate("USD", "MAD", "10.05")));

        BigDecimal result = currencyService.convert(new BigDecimal("100.00"), "USD", "MAD");

        assertEquals(0, new BigDecimal("1005.00").compareTo(result));
    }

    @Test
    @DisplayName("inverse rate divides when no direct rate exists")
    void inverseRate_divides() {
        lenient().when(exchangeRateDAO.findByCurrencies("EUR", "USD")).thenReturn(Optional.of(rate("EUR", "USD", "1.10")));

        BigDecimal result = currencyService.convert(new BigDecimal("110.00"), "USD", "EUR");

        assertEquals(0, new BigDecimal("100.00").compareTo(result));
    }

    @Test
    @DisplayName("cross rate through USD when only USD->X rates are stored")
    void crossRate_viaUsd() {
        // Stored like ExchangeRateService does: USD->EUR 0.80, USD->MAD 10.00, so 1 EUR = 12.50 MAD
        lenient().when(exchangeRateDAO.findByCurrencies("USD", "EUR")).thenReturn(Optional.of(rate("USD", "EUR", "0.80")));
        lenient().when(exchangeRateDAO.findByCurrencies("USD", "MAD")).thenReturn(Optional.of(rate("USD", "MAD", "10.00")));

        BigDecimal result = currencyService.convert(new BigDecimal("8.00"), "EUR", "MAD");

        assertEquals(0, new BigDecimal("100.00").compareTo(result));
    }

    @Test
    @DisplayName("no rate at all leaves the amount unchanged")
    void noRate_returnsOriginal() {
        BigDecimal amount = new BigDecimal("42.00");
        assertEquals(0, amount.compareTo(currencyService.convert(amount, "USD", "XYZ")));
    }

    @Test
    @DisplayName("display currency defaults to MAD and is configurable")
    void displayCurrency() {
        when(systemConfigDAO.getValue("DISPLAY_CURRENCY", "MAD")).thenReturn("MAD", "eur", "");
        assertEquals("MAD", currencyService.getDisplayCurrency());
        assertEquals("EUR", currencyService.getDisplayCurrency());
        assertEquals("MAD", currencyService.getDisplayCurrency());
    }
}
