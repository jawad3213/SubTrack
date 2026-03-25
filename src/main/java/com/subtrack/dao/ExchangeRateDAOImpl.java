package com.subtrack.dao;

import com.subtrack.entity.ExchangeRate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class ExchangeRateDAOImpl implements ExchangeRateDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void create(ExchangeRate exchangeRate) {
        em.persist(exchangeRate);
    }

    @Override
    public void update(ExchangeRate exchangeRate) {
        em.merge(exchangeRate);
    }

    @Override
    public void delete(ExchangeRate exchangeRate) {
        em.remove(em.contains(exchangeRate) ? exchangeRate : em.merge(exchangeRate));
    }

    @Override
    public Optional<ExchangeRate> findById(UUID id) {
        ExchangeRate exchangeRate = em.find(ExchangeRate.class, id);
        return Optional.ofNullable(exchangeRate);
    }

    @Override
    public List<ExchangeRate> findAll() {
        TypedQuery<ExchangeRate> query = em.createQuery(
            "SELECT er FROM ExchangeRate er ORDER BY er.fromCurrency, er.toCurrency", ExchangeRate.class);
        return query.getResultList();
    }

    @Override
    public Optional<ExchangeRate> findByCurrencies(String fromCurrency, String toCurrency) {
        TypedQuery<ExchangeRate> query = em.createQuery(
            "SELECT er FROM ExchangeRate er WHERE er.fromCurrency = :fromCurrency AND er.toCurrency = :toCurrency", 
            ExchangeRate.class);
        query.setParameter("fromCurrency", fromCurrency);
        query.setParameter("toCurrency", toCurrency);
        List<ExchangeRate> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<ExchangeRate> findByFromCurrency(String fromCurrency) {
        TypedQuery<ExchangeRate> query = em.createQuery(
            "SELECT er FROM ExchangeRate er WHERE er.fromCurrency = :fromCurrency ORDER BY er.toCurrency", 
            ExchangeRate.class);
        query.setParameter("fromCurrency", fromCurrency);
        return query.getResultList();
    }
}
