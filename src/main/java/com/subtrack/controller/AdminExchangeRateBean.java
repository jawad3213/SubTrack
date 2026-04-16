package com.subtrack.controller;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bean for Exchange Rate Management admin page.
 * On "Refresh All Rates" it calls the real exchange rate API and stores results.
 */
@Named
@ViewScoped
public class AdminExchangeRateBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @jakarta.inject.Inject
    private com.subtrack.service.ExchangeRateService exchangeRateService;

    private List<ExchangeRateDTO> exchangeRates = new ArrayList<>();
    private ExchangeRateDTO selectedRate;

    @jakarta.annotation.PostConstruct
    public void init() {
        loadRates();
    }

    private void loadRates() {
        exchangeRates.clear();
        List<com.subtrack.entity.ExchangeRate> rates = exchangeRateService.findAll();
        for (com.subtrack.entity.ExchangeRate r : rates) {
            boolean fresh = r.getUpdatedAt() != null
                    && r.getUpdatedAt().isAfter(LocalDateTime.now().minusHours(24));
            // Display as "X per 1 USD" — fromCurrency is always USD after a fetch
            String displayCurrency = "USD".equals(r.getFromCurrency())
                    ? r.getToCurrency()
                    : r.getFromCurrency();
            exchangeRates.add(new ExchangeRateDTO(displayCurrency, r.getRate(), r.getUpdatedAt(), fresh));
        }
    }

    public void refreshAllRates() {
        String result = exchangeRateService.fetchAndStoreAllRates();
        if (result.startsWith("SUCCESS")) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", result));
            loadRates();
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", result));
        }
    }

    // Getters and setters
    public List<ExchangeRateDTO> getExchangeRates() { return exchangeRates; }
    public ExchangeRateDTO getSelectedRate() { return selectedRate; }
    public void setSelectedRate(ExchangeRateDTO o) { this.selectedRate = o; }

    public static class ExchangeRateDTO {
        private String currencyCode;
        private BigDecimal rateToMad;
        private LocalDateTime lastUpdated;
        private boolean fresh;

        public ExchangeRateDTO(String currencyCode, BigDecimal rateToMad, LocalDateTime lastUpdated, boolean fresh) {
            this.currencyCode = currencyCode;
            this.rateToMad = rateToMad;
            this.lastUpdated = lastUpdated;
            this.fresh = fresh;
        }

        public String getCurrencyCode() { return currencyCode; }
        public BigDecimal getRateToMad() { return rateToMad; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        public boolean isFresh() { return fresh; }
    }
}
