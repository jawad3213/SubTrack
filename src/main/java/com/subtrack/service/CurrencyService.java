package com.subtrack.service;

import com.subtrack.dao.ExchangeRateDAO;
import com.subtrack.dao.SystemConfigDAO;
import com.subtrack.entity.ExchangeRate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Converts amounts between currencies using the stored exchange rates, so totals
 * that mix USD, EUR, MAD... subscriptions are summed in a single currency.
 */
@ApplicationScoped
public class CurrencyService {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CurrencyService.class);

    /** ExchangeRateService stores every rate as BASE_CURRENCY -> X. */
    static final String BASE_CURRENCY = "USD";
    static final String DEFAULT_DISPLAY_CURRENCY = "MAD";

    @Inject
    private ExchangeRateDAO exchangeRateDAO;

    @Inject
    private SystemConfigDAO systemConfigDAO;

    /** Currency all totals are shown in; configurable through the DISPLAY_CURRENCY system setting. */
    public String getDisplayCurrency() {
        String configured = systemConfigDAO.getValue("DISPLAY_CURRENCY", DEFAULT_DISPLAY_CURRENCY);
        return (configured == null || configured.isBlank()) ? DEFAULT_DISPLAY_CURRENCY : configured.trim().toUpperCase();
    }

    public BigDecimal toDisplayCurrency(BigDecimal amount, String fromCurrency) {
        return convert(amount, fromCurrency, getDisplayCurrency());
    }

    /**
     * Converts using a direct rate, the inverse rate, or a cross rate through USD.
     * If no rate is available the amount is returned unchanged and a warning is logged.
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (fromCurrency == null || toCurrency == null || fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }
        String from = fromCurrency.toUpperCase();
        String to = toCurrency.toUpperCase();

        Optional<BigDecimal> rate = rate(from, to);
        if (rate.isEmpty()) {
            Optional<BigDecimal> baseToFrom = rate(BASE_CURRENCY, from);
            Optional<BigDecimal> baseToTo = rate(BASE_CURRENCY, to);
            if (baseToFrom.isPresent() && baseToTo.isPresent()) {
                rate = Optional.of(baseToTo.get().divide(baseToFrom.get(), MathContext.DECIMAL64));
            }
        }

        if (rate.isEmpty()) {
            LOGGER.warn("No exchange rate for " + from + " -> " + to + "; amount left unconverted");
            return amount;
        }
        return amount.multiply(rate.get()).setScale(2, RoundingMode.HALF_UP);
    }

    private Optional<BigDecimal> rate(String from, String to) {
        if (from.equals(to)) {
            return Optional.of(BigDecimal.ONE);
        }
        Optional<BigDecimal> direct = exchangeRateDAO.findByCurrencies(from, to)
            .map(ExchangeRate::getRate)
            .filter(r -> r.signum() > 0);
        if (direct.isPresent()) {
            return direct;
        }
        return exchangeRateDAO.findByCurrencies(to, from)
            .map(ExchangeRate::getRate)
            .filter(r -> r.signum() > 0)
            .map(r -> BigDecimal.ONE.divide(r, MathContext.DECIMAL64));
    }
}
