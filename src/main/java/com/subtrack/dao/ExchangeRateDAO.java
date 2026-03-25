package com.subtrack.dao;

import com.subtrack.entity.ExchangeRate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRateDAO {
    
    void create(ExchangeRate exchangeRate);
    
    void update(ExchangeRate exchangeRate);
    
    void delete(ExchangeRate exchangeRate);
    
    Optional<ExchangeRate> findById(UUID id);
    
    List<ExchangeRate> findAll();
    
    Optional<ExchangeRate> findByCurrencies(String fromCurrency, String toCurrency);
    
    List<ExchangeRate> findByFromCurrency(String fromCurrency);
}
