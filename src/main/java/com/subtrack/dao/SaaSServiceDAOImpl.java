package com.subtrack.dao;

import com.subtrack.entity.SaaSService;
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
public class SaaSServiceDAOImpl implements SaaSServiceDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void create(SaaSService service) {
        em.persist(service);
    }

    @Override
    public void update(SaaSService service) {
        em.merge(service);
    }

    @Override
    public void delete(SaaSService service) {
        em.remove(em.contains(service) ? service : em.merge(service));
    }

    @Override
    public Optional<SaaSService> findById(UUID id) {
        SaaSService service = em.find(SaaSService.class, id);
        return Optional.ofNullable(service);
    }

    @Override
    public List<SaaSService> findAll() {
        TypedQuery<SaaSService> query = em.createQuery(
            "SELECT s FROM SaaSService s ORDER BY s.name", SaaSService.class);
        return query.getResultList();
    }

    @Override
    public List<SaaSService> findByNameContaining(String name) {
        TypedQuery<SaaSService> query = em.createQuery(
            "SELECT s FROM SaaSService s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))", 
            SaaSService.class);
        query.setParameter("name", name);
        return query.getResultList();
    }

    @Override
    public Optional<SaaSService> findByServiceName(String serviceName) {
        TypedQuery<SaaSService> query = em.createQuery(
            "SELECT s FROM SaaSService s WHERE s.name = :name", SaaSService.class);
        query.setParameter("name", serviceName);
        List<SaaSService> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
