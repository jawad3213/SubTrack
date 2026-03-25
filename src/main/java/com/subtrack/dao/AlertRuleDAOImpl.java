package com.subtrack.dao;

import com.subtrack.entity.AlertRule;
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
public class AlertRuleDAOImpl implements AlertRuleDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void create(AlertRule alertRule) {
        em.persist(alertRule);
    }

    @Override
    public void update(AlertRule alertRule) {
        em.merge(alertRule);
    }

    @Override
    public void delete(AlertRule alertRule) {
        em.remove(em.contains(alertRule) ? alertRule : em.merge(alertRule));
    }

    @Override
    public Optional<AlertRule> findById(UUID id) {
        AlertRule alertRule = em.find(AlertRule.class, id);
        return Optional.ofNullable(alertRule);
    }

    @Override
    public List<AlertRule> findAll() {
        TypedQuery<AlertRule> query = em.createQuery(
            "SELECT a FROM AlertRule a ORDER BY a.createdAt DESC", AlertRule.class);
        return query.getResultList();
    }

    @Override
    public List<AlertRule> findBySubscriptionId(UUID subscriptionId) {
        TypedQuery<AlertRule> query = em.createQuery(
            "SELECT a FROM AlertRule a WHERE a.subscription.id = :subscriptionId", AlertRule.class);
        query.setParameter("subscriptionId", subscriptionId);
        return query.getResultList();
    }

    @Override
    public List<AlertRule> findActiveAlertRules() {
        TypedQuery<AlertRule> query = em.createQuery(
            "SELECT a FROM AlertRule a WHERE a.isActive = true", AlertRule.class);
        return query.getResultList();
    }

    @Override
    public List<AlertRule> findAlertsDueSoon(int days) {
        TypedQuery<AlertRule> query = em.createQuery(
            "SELECT a FROM AlertRule a JOIN FETCH a.subscription s " +
            "WHERE a.isActive = true AND s.nextBillingDate IS NOT NULL " +
            "AND s.nextBillingDate <= :targetDate",
            AlertRule.class);
        query.setParameter("targetDate", java.time.LocalDate.now().plusDays(days));
        return query.getResultList();
    }
}
