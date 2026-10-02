package com.subtrack.service;

import com.subtrack.dao.ExchangeRateDAO;
import com.subtrack.dao.SystemConfigDAO;
import com.subtrack.entity.ExchangeRate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class ExchangeRateService {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(ExchangeRateService.class);

    @Inject
    private ExchangeRateDAO exchangeRateDAO;

    @Inject
    private SystemConfigDAO systemConfigDAO;
    
    
    

    @Inject
    private CurrencyService currencyService;

    public List<ExchangeRate> findAll() {
        return exchangeRateDAO.findAll();
    }

    public Optional<ExchangeRate> findByCurrencies(String fromCurrency, String toCurrency) {
        return exchangeRateDAO.findByCurrencies(fromCurrency, toCurrency);
    }

    /**
     * Fetches ALL currencies from the exchange rate API and stores them as USD->X rates.
     * Returns a result message to display in the UI.
     */
    @Transactional
    public String fetchAndStoreAllRates() {
        String apiKey = systemConfigDAO.getValue("EXCHANGE_RATE_API_KEY", "");
        String baseUrl = systemConfigDAO.getValue("EXCHANGE_RATE_URL", "");

        if (baseUrl == null || baseUrl.isBlank()) {
            return "ERROR: EXCHANGE_RATE_URL is not configured. Please set it in Admin → Configuration.";
        }

        try {
            String urlStr = baseUrl;
            // The exchangerate-api.com v6 format embeds the key in the URL path.
            // If the URL already contains the key (e.g. https://v6.exchangerate-api.com/v6/KEY/latest/USD)
            // we don't append it as a query param.
            if (!apiKey.isBlank() && !baseUrl.contains(apiKey) && !baseUrl.contains("?")) {
                urlStr = baseUrl + "?api_key=" + apiKey;
            }

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return "ERROR: API returned HTTP " + responseCode;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }

            String json = sb.toString();

            // Support both v4 format ("rates") and v6 format ("conversion_rates")
            Map<String, BigDecimal> rates = parseAllRates(json);

            if (rates.isEmpty()) {
                return "ERROR: Could not parse any rates from API response. Check the URL format.";
            }

            int saved = 0;
            for (Map.Entry<String, BigDecimal> entry : rates.entrySet()) {
                try {
                    ExchangeRate er = new ExchangeRate();
                    er.setFromCurrency("USD");
                    er.setToCurrency(entry.getKey());
                    er.setRate(entry.getValue());
                    save(er);
                    saved++;
                } catch (Exception e) {
                    LOGGER.warn("Failed to save rate for " + entry.getKey(), e);
                }
            }

            return "SUCCESS: Updated " + saved + " exchange rates from API.";

        } catch (Exception e) {
            LOGGER.error("fetchAndStoreAllRates failed", e);
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Parses the JSON response from exchangerate-api.com.
     * Supports both \"rates\": {} (v4) and \"conversion_rates\": {} (v6).
     */
    private Map<String, BigDecimal> parseAllRates(String json) {
        Map<String, BigDecimal> result = new HashMap<>();

        // Find the opening brace of the rates block
        String block = null;
        for (String key : new String[]{"\"conversion_rates\"", "\"rates\""}) {
            int idx = json.indexOf(key);
            if (idx != -1) {
                int open = json.indexOf('{', idx);
                int close = findMatchingBrace(json, open);
                if (open != -1 && close != -1) {
                    block = json.substring(open + 1, close);
                    break;
                }
            }
        }

        if (block == null) return result;

        // Parse key:value pairs from the block
        // Format: "USD": 1, "EUR": 0.848, ...
        int i = 0;
        while (i < block.length()) {
            int q1 = block.indexOf('"', i);
            if (q1 == -1) break;
            int q2 = block.indexOf('"', q1 + 1);
            if (q2 == -1) break;
            String currency = block.substring(q1 + 1, q2);
            int colon = block.indexOf(':', q2);
            if (colon == -1) break;
            int commaOr = block.indexOf(',', colon);
            int braceOr = block.indexOf('}', colon);
            int end = Math.min(
                commaOr > 0 ? commaOr : Integer.MAX_VALUE,
                braceOr > 0 ? braceOr : Integer.MAX_VALUE
            );
            if (end == Integer.MAX_VALUE) break;
            String valueStr = block.substring(colon + 1, end).trim();
            try {
                result.put(currency, new BigDecimal(valueStr));
            } catch (NumberFormatException ignored) {}
            i = end + 1;
        }

        return result;
    }

    private int findMatchingBrace(String json, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < json.length(); i++) {
            if (json.charAt(i) == '{') depth++;
            else if (json.charAt(i) == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    @Transactional
    public void save(ExchangeRate exchangeRate) {
        Optional<ExchangeRate> existing = exchangeRateDAO.findByCurrencies(
            exchangeRate.getFromCurrency(), exchangeRate.getToCurrency());
        if (existing.isPresent()) {
            existing.get().setRate(exchangeRate.getRate());
            exchangeRateDAO.update(existing.get());
        } else {
            exchangeRateDAO.create(exchangeRate);
        }
    }

    public BigDecimal convertToLocalCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        return currencyService.convert(amount, fromCurrency, toCurrency);
    }
}
