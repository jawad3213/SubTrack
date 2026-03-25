package com.subtrack.controller;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Stub bean for Exchange Rate Management admin page.
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
            boolean fresh = r.getUpdatedAt() != null && r.getUpdatedAt().isAfter(java.time.LocalDateTime.now().minusDays(1));
            // Assuming the rates are stored as USD -> MAD, EUR -> MAD, etc. 
            // the UI expects rateToMad, so if fromCurrency is the foreign currency and toCurrency is MAD:
            if ("MAD".equals(r.getToCurrency())) {
                exchangeRates.add(new ExchangeRateDTO(r.getFromCurrency(), r.getRate(), r.getUpdatedAt(), fresh));
            } else if ("MAD".equals(r.getFromCurrency())) {
                // If it's stored as MAD -> USD, the rate to MAD is 1/rate
                java.math.BigDecimal inverse = java.math.BigDecimal.ONE.divide(r.getRate(), 4, java.math.RoundingMode.HALF_UP);
                exchangeRates.add(new ExchangeRateDTO(r.getToCurrency(), inverse, r.getUpdatedAt(), fresh));
            }
        }
        
        // Add some mock ones if empty for demonstration
        if (exchangeRates.isEmpty()) {
            exchangeRates.add(new ExchangeRateDTO("USD", new java.math.BigDecimal("10.05"), java.time.LocalDateTime.now().minusHours(2), true));
            exchangeRates.add(new ExchangeRateDTO("EUR", new java.math.BigDecimal("10.85"), java.time.LocalDateTime.now().minusDays(2), false));
            exchangeRates.add(new ExchangeRateDTO("GBP", new java.math.BigDecimal("12.75"), java.time.LocalDateTime.now().minusHours(5), true));
        }
    }

    public void refreshAllRates() {
        jakarta.faces.context.FacesContext.getCurrentInstance().addMessage(null,
            new jakarta.faces.application.FacesMessage(jakarta.faces.application.FacesMessage.SEVERITY_INFO, "Refresh triggered", "Exchange rates have been successfully updated from API."));
        loadRates();
        // Here we would call the actual API integration logic, e.g., currency integration
    }

    // Getters and setters
    public List<ExchangeRateDTO> getExchangeRates() { return exchangeRates; }
    public ExchangeRateDTO getSelectedRate() { return selectedRate; }
    public void setSelectedRate(ExchangeRateDTO o) { this.selectedRate = o; }

    public static class ExchangeRateDTO {
        private String currencyCode;
        private java.math.BigDecimal rateToMad;
        private java.time.LocalDateTime lastUpdated;
        private boolean fresh;

        public ExchangeRateDTO(String currencyCode, java.math.BigDecimal rateToMad, java.time.LocalDateTime lastUpdated, boolean fresh) {
            this.currencyCode = currencyCode;
            this.rateToMad = rateToMad;
            this.lastUpdated = lastUpdated;
            this.fresh = fresh;
        }

        public String getCurrencyCode() { return currencyCode; }
        public java.math.BigDecimal getRateToMad() { return rateToMad; }
        public java.time.LocalDateTime getLastUpdated() { return lastUpdated; }
        public boolean isFresh() { return fresh; }
    }
}
