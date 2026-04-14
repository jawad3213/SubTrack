package com.subtrack.dao;

import com.subtrack.entity.SystemLog;
import java.util.List;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class SystemLogDAO {

    @PersistenceContext
    private EntityManager em;

    public void create(SystemLog log) {
        em.persist(log);
    }

    public List<SystemLog> findAll() {
        TypedQuery<SystemLog> query = em.createQuery(
            "SELECT s FROM SystemLog s ORDER BY s.loggedAt DESC", SystemLog.class);
        return query.getResultList();
    }

    public List<SystemLog> findByLevel(String level) {
        TypedQuery<SystemLog> query = em.createQuery(
            "SELECT s FROM SystemLog s WHERE s.level = :level ORDER BY s.loggedAt DESC", SystemLog.class);
        query.setParameter("level", level);
        return query.getResultList();
    }

    public List<SystemLog> findByKeyword(String keyword) {
        TypedQuery<SystemLog> query = em.createQuery(
            "SELECT s FROM SystemLog s WHERE s.message LIKE :keyword OR s.logger LIKE :keyword ORDER BY s.loggedAt DESC", SystemLog.class);
        query.setParameter("keyword", "%" + keyword + "%");
        return query.getResultList();
    }

    public List<SystemLog> findByLevelAndKeyword(String level, String keyword) {
        TypedQuery<SystemLog> query = em.createQuery(
            "SELECT s FROM SystemLog s WHERE s.level = :level AND (s.message LIKE :keyword OR s.logger LIKE :keyword) ORDER BY s.loggedAt DESC", SystemLog.class);
        query.setParameter("level", level);
        query.setParameter("keyword", "%" + keyword + "%");
        return query.getResultList();
    }

    public int deleteOldLogs(int daysToKeep) {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(daysToKeep);
        int deleted = em.createQuery("DELETE FROM SystemLog s WHERE s.loggedAt < :cutoff")
            .setParameter("cutoff", cutoff)
            .executeUpdate();
        return deleted;
    }
}