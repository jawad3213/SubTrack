package com.subtrack.service;

import com.subtrack.dao.SystemConfigDAO;
import com.subtrack.entity.ExchangeRate;
import com.subtrack.util.AppLogger;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class ExchangeRateScheduler {

    private static final Logger LOGGER = Logger.getLogger(ExchangeRateScheduler.class.getName());
    private static final AppLogger APP_LOGGER = AppLogger.getLogger(ExchangeRateScheduler.class);

    @Inject
    private ExchangeRateService exchangeRateService;
    
    @Inject
    private SystemConfigDAO systemConfigDAO;
    
    @Inject
    private WebhookService webhookService;
    
    private static final String[] TARGET_CURRENCIES = {"EUR", "GBP", "JPY", "CAD", "AUD", "MAD"};
    
    @Schedule(hour = "*/6", minute = "0")
    public void fetchExchangeRates() {
        String apiKey = systemConfigDAO.getValue("EXCHANGE_RATE_API_KEY", "");
        String baseUrl = systemConfigDAO.getValue("EXCHANGE_RATE_URL", "https://api.exchangerate-api.com/v4/latest/USD");
        
        if (baseUrl.isBlank()) {
            LOGGER.warning("ExchangeRateScheduler: EXCHANGE_RATE_URL not configured");
            APP_LOGGER.logError("EXCHANGE_RATE_URL not configured");
            return;
        }
        
        try {
            String urlStr = baseUrl;
            if (!apiKey.isBlank() && !baseUrl.contains("?")) {
                urlStr = baseUrl + "?api_key=" + apiKey;
            }
            
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                LOGGER.log(Level.WARNING, "ExchangeRateScheduler: API returned HTTP " + responseCode);
                APP_LOGGER.logError("Exchange Rate API returned HTTP " + responseCode);
                webhookService.notifyExchangeRateError("API returned HTTP " + responseCode);
                return;
            }
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            
            Map<String, BigDecimal> rates = parseRatesFromResponse(response.toString());
            saveRatesToDatabase(rates);
            
            LOGGER.info("ExchangeRateScheduler: Updated " + rates.size() + " exchange rates");
            APP_LOGGER.logInfo("Updated " + rates.size() + " exchange rates");
            webhookService.notifyExchangeRateSuccess(rates.size());
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "ExchangeRateScheduler: Failed to fetch rates", e);
            APP_LOGGER.logError("Failed to fetch exchange rates: " + e.getMessage());
            webhookService.notifyExchangeRateError("Connection failed: " + e.getMessage());
        }
    }
    
    private Map<String, BigDecimal> parseRatesFromResponse(String json) {
        Map<String, BigDecimal> rates = new HashMap<>();
        
        for (String currency : TARGET_CURRENCIES) {
            String field = "\"" + currency + "\"";
            int idx = json.indexOf(field);
            if (idx != -1) {
                int colon = json.indexOf(":", idx);
                int comma = json.indexOf(",", colon);
                int brace = json.indexOf("}", colon);
                int end = Math.min(comma > 0 ? comma : Integer.MAX_VALUE, brace > 0 ? brace : Integer.MAX_VALUE);
                if (end > colon) {
                    String valueStr = json.substring(colon + 1, end).trim();
                    try {
                        rates.put(currency, new BigDecimal(valueStr));
                    } catch (NumberFormatException e) {
                        // skip invalid values
                    }
                }
            }
        }
        
        return rates;
    }
    
    private void saveRatesToDatabase(Map<String, BigDecimal> rates) {
        int savedCount = 0;
        for (Map.Entry<String, BigDecimal> entry : rates.entrySet()) {
            try {
                ExchangeRate rate = new ExchangeRate();
                rate.setFromCurrency("USD");
                rate.setToCurrency(entry.getKey());
                rate.setRate(entry.getValue());
                exchangeRateService.save(rate);
                savedCount++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "ExchangeRateScheduler: Failed to save rate for " + entry.getKey(), e);
            }
        }
        LOGGER.info("ExchangeRateScheduler: Saved " + savedCount + " rates to database");
    }
}