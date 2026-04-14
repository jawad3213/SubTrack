package com.subtrack.dao;

import com.subtrack.entity.NotificationLog;
import com.subtrack.enums.AlertChannel;
import java.util.List;
import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class NotificationLogDAO {

    @PersistenceContext
    private EntityManager em;

    public void create(NotificationLog log) {
        em.persist(log);
    }

    public List<NotificationLog> findAll() {
        TypedQuery<NotificationLog> query = em.createQuery(
            "SELECT n FROM NotificationLog n ORDER BY n.sentAt DESC", NotificationLog.class);
        return query.getResultList();
    }

    public List<NotificationLog> findByChannel(AlertChannel channel) {
        TypedQuery<NotificationLog> query = em.createQuery(
            "SELECT n FROM NotificationLog n WHERE n.channel = :channel ORDER BY n.sentAt DESC", NotificationLog.class);
        query.setParameter("channel", channel);
        return query.getResultList();
    }

    public List<NotificationLog> findByDeliveryStatus(String status) {
        TypedQuery<NotificationLog> query = em.createQuery(
            "SELECT n FROM NotificationLog n WHERE n.deliveryStatus = :status ORDER BY n.sentAt DESC", NotificationLog.class);
        query.setParameter("status", status);
        return query.getResultList();
    }

    public List<NotificationLog> findByChannelAndStatus(AlertChannel channel, String status) {
        TypedQuery<NotificationLog> query = em.createQuery(
            "SELECT n FROM NotificationLog n WHERE n.channel = :channel AND n.deliveryStatus = :status ORDER BY n.sentAt DESC", NotificationLog.class);
        query.setParameter("channel", channel);
        query.setParameter("status", status);
        return query.getResultList();
    }
}