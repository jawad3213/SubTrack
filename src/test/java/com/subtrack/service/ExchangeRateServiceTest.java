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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ExchangeRateService}.
 * Covers save/upsert logic and the parseAllRates JSON parser (conversion is in CurrencyServiceTest).
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