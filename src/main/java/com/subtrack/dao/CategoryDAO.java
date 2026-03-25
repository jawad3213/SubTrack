package com.subtrack.dao;

import com.subtrack.entity.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryDAO {
    
    void create(Category category);
    
    void update(Category category);
    
    void delete(Category category);
    
    Optional<Category> findById(UUID id);
    
    Optional<Category> findByName(String name);
    
    List<Category> findAll();
    
    List<Category> findAllOrderByName();
    
    boolean existsByName(String name);
}
