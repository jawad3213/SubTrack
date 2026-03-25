package com.subtrack.service;

import com.subtrack.dao.SaaSServiceDAO;
import com.subtrack.entity.SaaSService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SaaSCatalogService {

    @Inject
    private SaaSServiceDAO saasServiceDAO;

    public List<SaaSService> getAllServices() {
        return saasServiceDAO.findAll();
    }

    public Optional<SaaSService> getServiceById(UUID id) {
        return saasServiceDAO.findById(id);
    }

    public List<SaaSService> searchServices(String name) {
        return saasServiceDAO.findByNameContaining(name);
    }

    @Transactional
    public void createService(SaaSService service) {
        saasServiceDAO.create(service);
    }

    @Transactional
    public void updateService(SaaSService service) {
        saasServiceDAO.update(service);
    }

    @Transactional
    public void deleteService(UUID id) {
        saasServiceDAO.findById(id).ifPresent(saasServiceDAO::delete);
    }

    @Transactional
    public void addServiceToCatalog(String name, String description, String logoUrl, String category) {
        SaaSService service = new SaaSService();
        service.setName(name);
        service.setDescription(description);
        service.setLogoUrl(logoUrl);
        service.setCategory(category);
        saasServiceDAO.create(service);
    }
}
