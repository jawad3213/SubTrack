package com.subtrack.dao;

import com.subtrack.entity.Subscription;
import com.subtrack.enums.SubscriptionStatus;
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
public class SubscriptionDAOImpl implements SubscriptionDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void create(Subscription subscription) {
        em.persist(subscription);
    }

    @Override
    public void update(Subscription subscription) {
        em.merge(subscription);
    }

    @Override
    public void delete(Subscription subscription) {
        em.remove(em.contains(subscription) ? subscription : em.merge(subscription));
    }

    @Override
    public Optional<Subscription> findById(UUID id) {
        Subscription subscription = em.find(Subscription.class, id);
        return Optional.ofNullable(subscription);
    }

    @Override
    public List<Subscription> findByClientId(UUID clientId) {
        TypedQuery<Subscription> query = em.createQuery(
            "SELECT s FROM Subscription s WHERE s.client.id = :clientId ORDER BY s.createdAt DESC", Subscription.class);
        query.setParameter("clientId", clientId);
        return query.getResultList();
    }

    @Override
    public List<Subscription> findAll() {
        TypedQuery<Subscription> query = em.createQuery(
            "SELECT s FROM Subscription s ORDER BY s.createdAt DESC", Subscription.class);
        return query.getResultList();
    }
    
    @Override
    public List<Subscription> findByClientIdAndStatus(UUID clientId, SubscriptionStatus status) {
        TypedQuery<Subscription> query = em.createQuery(
            "SELECT s FROM Subscription s WHERE s.client.id = :clientId AND s.status = :status ORDER BY s.createdAt DESC", 
            Subscription.class);
        query.setParameter("clientId", clientId);
        query.setParameter("status", status);
        return query.getResultList();
    }
    
    @Override
    public List<Subscription> findByClientIdAndCategoryId(UUID clientId, UUID categoryId) {
        TypedQuery<Subscription> query = em.createQuery(
            "SELECT s FROM Subscription s LEFT JOIN s.category c WHERE s.client.id = :clientId AND c.id = :categoryId ORDER BY s.createdAt DESC",
            Subscription.class);
        query.setParameter("clientId", clientId);
        query.setParameter("categoryId", categoryId);
        return query.getResultList();
    }
    
    @Override
    public List<Subscription> findByNameContaining(String name) {
        TypedQuery<Subscription> query = em.createQuery(
            "SELECT s FROM Subscription s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY s.name", 
            Subscription.class);
        query.setParameter("name", name);
        return query.getResultList();
    }
}
