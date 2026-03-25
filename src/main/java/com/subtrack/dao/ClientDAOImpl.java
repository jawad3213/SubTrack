package com.subtrack.dao;

import com.subtrack.entity.Client;
import com.subtrack.enums.Role;
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
public class ClientDAOImpl implements ClientDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void create(Client client) {
        em.persist(client);
    }

    @Override
    public void update(Client client) {
        em.merge(client);
    }

    @Override
    public void delete(Client client) {
        em.remove(em.contains(client) ? client : em.merge(client));
    }

    @Override
    public Optional<Client> findById(UUID id) {
        Client client = em.find(Client.class, id);
        return Optional.ofNullable(client);
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        TypedQuery<Client> query = em.createQuery(
            "SELECT c FROM Client c WHERE c.email = :email", Client.class);
        query.setParameter("email", email);
        List<Client> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<Client> findAll() {
        TypedQuery<Client> query = em.createQuery(
            "SELECT c FROM Client c", Client.class);
        return query.getResultList();
    }

    @Override
    public List<Client> findByRole(Role role) {
        TypedQuery<Client> query = em.createQuery(
            "SELECT c FROM Client c WHERE c.role = :role", Client.class);
        query.setParameter("role", role);
        return query.getResultList();
    }

    @Override
    public List<Client> findActiveClients() {
        TypedQuery<Client> query = em.createQuery(
            "SELECT c FROM Client c WHERE c.isActive = true", Client.class);
        return query.getResultList();
    }

    @Override
    public boolean existsByEmail(String email) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(c) FROM Client c WHERE c.email = :email", Long.class);
        query.setParameter("email", email);
        return query.getSingleResult() > 0;
    }
}
