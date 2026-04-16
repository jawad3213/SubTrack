package com.subtrack.service;

import com.subtrack.dao.ExchangeRateDAO;
import com.subtrack.dao.SystemConfigDAO;
import com.subtrack.entity.ExchangeRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ExchangeRateService}.
 * Covers currency conversion, save/upsert logic, and the parseAllRates JSON parser.
 */
@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateDAO exchangeRateDAO;

    @Mock
    private SystemConfigDAO systemConfigDAO;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    // ---------------------------------------------------------------
    // convertToLocalCurrency
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("convertToLocalCurrency()")
    class ConvertCurrency {

        @Test
        @DisplayName("should return same amount when currencies are identical")
        void sameCurrency_returnsOriginal() {
            BigDecimal amount = new BigDecimal("100.00");
            BigDecimal result = exchangeRateService.convertToLocalCurrency(amount, "USD", "USD");
            assertEquals(0, amount.compareTo(result));
        }

        @Test
        @DisplayName("should return amount unchanged when fromCurrency is null")
        void nullFrom_returnsOriginal() {
            BigDecimal amount = new BigDecimal("50.00");
            BigDecimal result = exchangeRateService.convertToLocalCurrency(amount, null, "EUR");
            assertEquals(0, amount.compareTo(result));
        }

        @Test
        @DisplayName("should return amount unchanged when toCurrency is null")
        void nullTo_returnsOriginal() {
            BigDecimal amount = new BigDecimal("50.00");
            BigDecimal result = exchangeRateService.convertToLocalCurrency(amount, "USD", null);
            assertEquals(0, amount.compareTo(result));
        }

        @Test
        @DisplayName("should multiply by direct rate when available")
        void directRate_multiplies() {
            ExchangeRate rate = new ExchangeRate();
            rate.setFromCurrency("USD");
            rate.setToCurrency("MAD");
            rate.setRate(new BigDecimal("10.05"));
            when(exchangeRateDAO.findByCurrencies("USD", "MAD")).thenReturn(Optional.of(rate));

            BigDecimal result = exchangeRateService.convertToLocalCurrency(
                    new BigDecimal("100.00"), "USD", "MAD");

            assertEquals(0, new BigDecimal("1005.00").compareTo(result));
        }

        @Test
        @DisplayName("should use inverse rate when direct rate is not found")
        void inverseRate_divides() {
            // No direct USD->EUR, but EUR->USD exists at rate 1.10
            when(exchangeRateDAO.findByCurrencies("USD", "EUR")).thenReturn(Optional.empty());
            ExchangeRate inverse = new ExchangeRate();
            inverse.setFromCurrency("EUR");
            inverse.setToCurrency("USD");
            inverse.setRate(new BigDecimal("1.10"));
            when(exchangeRateDAO.findByCurrencies("EUR", "USD")).thenReturn(Optional.of(inverse));

            BigDecimal result = exchangeRateService.convertToLocalCurrency(
                    new BigDecimal("110.00"), "USD", "EUR");

            // 110 / 1.10 = 100.00
            assertEquals(0, new BigDecimal("100.00").compareTo(result));
        }

        @Test
        @DisplayName("should return original amount when no rate exists at all")
        void noRate_returnsOriginal() {
            when(exchangeRateDAO.findByCurrencies("USD", "XYZ")).thenReturn(Optional.empty());
            when(exchangeRateDAO.findByCurrencies("XYZ", "USD")).thenReturn(Optional.empty());

            BigDecimal amount = new BigDecimal("42.00");
            BigDecimal result = exchangeRateService.convertToLocalCurrency(amount, "USD", "XYZ");

            assertEquals(0, amount.compareTo(result));
        }
    }

    // ---------------------------------------------------------------
    // save (upsert)
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("should update existing rate when currencies already exist")
        void save_existing_updates() {
            ExchangeRate existing = new ExchangeRate();
            existing.setFromCurrency("USD");
            existing.setToCurrency("EUR");
            existing.setRate(new BigDecimal("0.85"));
            when(exchangeRateDAO.findByCurrencies("USD", "EUR")).thenReturn(Optional.of(existing));

            ExchangeRate newRate = new ExchangeRate();
            newRate.setFromCurrency("USD");
            newRate.setToCurrency("EUR");
            newRate.setRate(new BigDecimal("0.90"));

            exchangeRateService.save(newRate);

            assertEquals(0, new BigDecimal("0.90").compareTo(existing.getRate()));
            verify(exchangeRateDAO).update(existing);
            verify(exchangeRateDAO, never()).create(any());
        }

        @Test
        @DisplayName("should create a new record when no existing rate is found")
        void save_new_creates() {
            when(exchangeRateDAO.findByCurrencies("USD", "GBP")).thenReturn(Optional.empty());

            ExchangeRate newRate = new ExchangeRate();
            newRate.setFromCurrency("USD");
            newRate.setToCurrency("GBP");
            newRate.setRate(new BigDecimal("0.75"));

            exchangeRateService.save(newRate);

            verify(exchangeRateDAO).create(newRate);
            verify(exchangeRateDAO, never()).update(any());
        }
    }

    // ---------------------------------------------------------------
    // fetchAndStoreAllRates – config-check paths (no HTTP)
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("fetchAndStoreAllRates() – configuration checks")
    class FetchAndStoreConfig {

        @Test
        @DisplayName("should return ERROR when EXCHANGE_RATE_URL is blank")
        void noUrl_returnsError() {
            when(systemConfigDAO.getValue("EXCHANGE_RATE_API_KEY", "")).thenReturn("");
            when(systemConfigDAO.getValue("EXCHANGE_RATE_URL", "")).thenReturn("");

            String result = exchangeRateService.fetchAndStoreAllRates();

            assertTrue(result.startsWith("ERROR"));
            assertTrue(result.contains("not configured"));
        }

        @Test
        @DisplayName("should return ERROR when EXCHANGE_RATE_URL is null")
        void nullUrl_returnsError() {
            when(systemConfigDAO.getValue("EXCHANGE_RATE_API_KEY", "")).thenReturn("");
            when(systemConfigDAO.getValue("EXCHANGE_RATE_URL", "")).thenReturn(null);

            String result = exchangeRateService.fetchAndStoreAllRates();

            assertTrue(result.startsWith("ERROR"));
        }
    }
}