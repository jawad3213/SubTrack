package com.subtrack.dao;

import com.subtrack.entity.Category;
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
public class CategoryDAOImpl implements CategoryDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void create(Category category) {
        em.persist(category);
    }

    @Override
    public void update(Category category) {
        em.merge(category);
    }

    @Override
    public void delete(Category category) {
        em.remove(em.contains(category) ? category : em.merge(category));
    }

    @Override
    public Optional<Category> findById(UUID id) {
        Category category = em.find(Category.class, id);
        return Optional.ofNullable(category);
    }

    @Override
    public Optional<Category> findByName(String name) {
        TypedQuery<Category> query = em.createQuery(
            "SELECT c FROM Category c WHERE c.name = :name", Category.class);
        query.setParameter("name", name);
        List<Category> results = query.getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<Category> findAll() {
        TypedQuery<Category> query = em.createQuery(
            "SELECT c FROM Category c", Category.class);
        return query.getResultList();
    }

    @Override
    public List<Category> findAllOrderByName() {
        TypedQuery<Category> query = em.createQuery(
            "SELECT c FROM Category c ORDER BY c.name", Category.class);
        return query.getResultList();
    }

    @Override
    public boolean existsByName(String name) {
        TypedQuery<Long> query = em.createQuery(
            "SELECT COUNT(c) FROM Category c WHERE c.name = :name", Long.class);
        query.setParameter("name", name);
        return query.getSingleResult() > 0;
    }
}
