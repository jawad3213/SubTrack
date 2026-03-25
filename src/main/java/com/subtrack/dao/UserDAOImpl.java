package com.subtrack.dao;

import com.subtrack.entity.User;
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
public class UserDAOImpl implements UserDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void create(User user) {
        em.persist(user);
    }

    @Override
    public void update(User user) {
        em.merge(user);
    }

    @Override
    public void delete(User user) {
        em.remove(em.contains(user) ? user : em.merge(user));
    }

    @Override
    public Optional<User> findById(UUID id) {
        User user = em.find(User.class, id);
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        TypedQuery<User> query = em.createQuery(
            "SELECT u FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", email);
        List<User> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<User> findAll() {
        TypedQuery<User> query = em.createQuery(
            "SELECT u FROM User u", User.class);
        return query.getResultList();
    }

    @Override
    public List<User> findByRole(Role role) {
        TypedQuery<User> query = em.createQuery(
            "SELECT u FROM User u WHERE u.role = :role", User.class);
        query.setParameter("role", role);
        return query.getResultList();
    }

    @Override
    public boolean existsByEmail(String email) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class);
        query.setParameter("email", email);
        return query.getSingleResult() > 0;
    }
}
