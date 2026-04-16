package com.subtrack.dao;

import com.subtrack.entity.Invoice;
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
public class InvoiceDAOImpl implements InvoiceDAO {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void create(Invoice invoice) {
        em.persist(invoice);
    }

    @Override
    public void update(Invoice invoice) {
        em.merge(invoice);
    }

    @Override
    public void delete(Invoice invoice) {
        em.remove(em.contains(invoice) ? invoice : em.merge(invoice));
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        Invoice invoice = em.find(Invoice.class, id);
        return Optional.ofNullable(invoice);
    }

    @Override
    public List<Invoice> findAll() {
        TypedQuery<Invoice> query = em.createQuery(
            "SELECT i FROM Invoice i ORDER BY i.createdAt DESC", Invoice.class);
        return query.getResultList();
    }

    @Override
    public List<Invoice> findByClientId(UUID clientId) {
        TypedQuery<Invoice> query = em.createQuery(
            "SELECT i FROM Invoice i JOIN FETCH i.client WHERE i.client.id = :clientId ORDER BY i.createdAt DESC", 
            Invoice.class);
        query.setParameter("clientId", clientId);
        return query.getResultList();
    }

    @Override
    public List<Invoice> findUnprocessedInvoices() {
        TypedQuery<Invoice> query = em.createQuery(
            "SELECT i FROM Invoice i WHERE i.isProcessed = false ORDER BY i.createdAt ASC", 
            Invoice.class);
        return query.getResultList();
    }

    @Override
    public List<Invoice> findByDateRange(LocalDate startDate, LocalDate endDate) {
        TypedQuery<Invoice> query = em.createQuery(
            "SELECT i FROM Invoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate ORDER BY i.invoiceDate DESC", 
            Invoice.class);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        return query.getResultList();
    }
}
