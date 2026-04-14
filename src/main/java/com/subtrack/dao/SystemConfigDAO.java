package com.subtrack.dao;

import com.subtrack.entity.SystemConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class SystemConfigDAO {

    @PersistenceContext(unitName = "subtrackPU")
    private EntityManager em;

    public String getValue(String key) {
        return getValue(key, null);
    }

    public String getValue(String key, String defaultValue) {
        SystemConfig config = em.find(SystemConfig.class, key);
        if (config != null && config.getConfigValue() != null) {
            return config.getConfigValue();
        }
        return defaultValue;
    }

    @Transactional
    public void setValue(String key, String value) {
        SystemConfig config = em.find(SystemConfig.class, key);
        if (config == null) {
            config = new SystemConfig(key, value);
            em.persist(config);
        } else {
            config.setConfigValue(value);
            config.setUpdatedAt(LocalDateTime.now());
            em.merge(config);
        }
    }

    public List<SystemConfig> findAll() {
        return em.createQuery("SELECT s FROM SystemConfig s", SystemConfig.class).getResultList();
    }
}
