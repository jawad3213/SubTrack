package com.subtrack.dao;

import com.subtrack.entity.Invoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceDAO {
    
    void create(Invoice invoice);
    
    void update(Invoice invoice);
    
    void delete(Invoice invoice);
    
    Optional<Invoice> findById(UUID id);
    
    List<Invoice> findAll();
    
    List<Invoice> findByClientId(UUID clientId);
    
    List<Invoice> findUnprocessedInvoices();
    
    List<Invoice> findByDateRange(LocalDate startDate, LocalDate endDate);
}
