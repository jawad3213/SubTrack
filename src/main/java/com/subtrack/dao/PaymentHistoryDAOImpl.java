package com.subtrack.dao;

import com.subtrack.entity.PaymentHistory;
import java.time.LocalDate;
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
public class PaymentHistoryDAOImpl implements PaymentHistoryDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void create(PaymentHistory paymentHistory) {
        em.persist(paymentHistory);
    }

    @Override
    public void update(PaymentHistory paymentHistory) {
        em.merge(paymentHistory);
    }

    @Override
    public void delete(PaymentHistory paymentHistory) {
        em.remove(em.contains(paymentHistory) ? paymentHistory : em.merge(paymentHistory));
    }

    @Override
    public Optional<PaymentHistory> findById(UUID id) {
        PaymentHistory paymentHistory = em.find(PaymentHistory.class, id);
        return Optional.ofNullable(paymentHistory);
    }

    @Override
    public List<PaymentHistory> findAll() {
        TypedQuery<PaymentHistory> query = em.createQuery(
            "SELECT ph FROM PaymentHistory ph ORDER BY ph.paymentDate DESC", PaymentHistory.class);
        return query.getResultList();
    }

    @Override
    public List<PaymentHistory> findBySubscriptionId(UUID subscriptionId) {
        TypedQuery<PaymentHistory> query = em.createQuery(
            "SELECT ph FROM PaymentHistory ph WHERE ph.subscription.id = :subscriptionId ORDER BY ph.paymentDate DESC", 
            PaymentHistory.class);
        query.setParameter("subscriptionId", subscriptionId);
        return query.getResultList();
    }

    @Override
    public List<PaymentHistory> findByDateRange(LocalDate startDate, LocalDate endDate) {
        TypedQuery<PaymentHistory> query = em.createQuery(
            "SELECT ph FROM PaymentHistory ph WHERE ph.paymentDate BETWEEN :startDate AND :endDate ORDER BY ph.paymentDate DESC", 
            PaymentHistory.class);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        return query.getResultList();
    }

    @Override
    public List<PaymentHistory> findBySubscriptionIdAndDateRange(UUID subscriptionId, LocalDate startDate, LocalDate endDate) {
        TypedQuery<PaymentHistory> query = em.createQuery(
            "SELECT ph FROM PaymentHistory ph WHERE ph.subscription.id = :subscriptionId AND ph.paymentDate BETWEEN :startDate AND :endDate ORDER BY ph.paymentDate DESC", 
            PaymentHistory.class);
        query.setParameter("subscriptionId", subscriptionId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        return query.getResultList();
    }
}
