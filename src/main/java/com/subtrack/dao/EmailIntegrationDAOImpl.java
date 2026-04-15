package com.subtrack.dao;

import com.subtrack.entity.EmailIntegration;
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
public class EmailIntegrationDAOImpl implements EmailIntegrationDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void create(EmailIntegration emailIntegration) {
        em.persist(emailIntegration);
    }

    @Override
    public void update(EmailIntegration emailIntegration) {
        em.merge(emailIntegration);
    }

    @Override
    public void delete(EmailIntegration emailIntegration) {
        em.remove(em.contains(emailIntegration) ? emailIntegration : em.merge(emailIntegration));
    }

    @Override
    public Optional<EmailIntegration> findById(UUID id) {
        EmailIntegration emailIntegration = em.find(EmailIntegration.class, id);
        return Optional.ofNullable(emailIntegration);
    }

    @Override
    public Optional<EmailIntegration> findByClientId(UUID clientId) {
        TypedQuery<EmailIntegration> query = em.createQuery(
            "SELECT e FROM EmailIntegration e JOIN FETCH e.client WHERE e.client.id = :clientId", EmailIntegration.class);
        query.setParameter("clientId", clientId);
        List<EmailIntegration> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<EmailIntegration> findAll() {
        TypedQuery<EmailIntegration> query = em.createQuery(
            "SELECT e FROM EmailIntegration e", EmailIntegration.class);
        return query.getResultList();
    }

    @Override
    public List<EmailIntegration> findActiveIntegrations() {
        TypedQuery<EmailIntegration> query = em.createQuery(
            "SELECT e FROM EmailIntegration e WHERE e.isActive = true", EmailIntegration.class);
        return query.getResultList();
    }
}
