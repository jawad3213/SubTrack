package com.subtrack.service;

import com.subtrack.dao.CategoryDAO;
import com.subtrack.entity.Category;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CategoryService {

    @Inject
    private CategoryDAO categoryDAO;

    public List<Category> findAll() {
        return categoryDAO.findAll();
    }

    public List<Category> findAllOrderByName() {
        return categoryDAO.findAllOrderByName();
    }

    public Optional<Category> findById(UUID id) {
        return categoryDAO.findById(id);
    }

    public Optional<Category> findByName(String name) {
        return categoryDAO.findByName(name);
    }

    public void create(Category category) {
        categoryDAO.create(category);
    }

    public void update(Category category) {
        categoryDAO.update(category);
    }

    public void delete(Category category) {
        categoryDAO.delete(category);
    }
}
