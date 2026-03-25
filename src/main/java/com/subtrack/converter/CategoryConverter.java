package com.subtrack.converter;

import com.subtrack.entity.Category;
import com.subtrack.service.CategoryService;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;
import java.util.UUID;

@FacesConverter(value = "categoryConverter", managed = true)
public class CategoryConverter implements Converter<Category> {

    @Inject
    private CategoryService categoryService;

    @Override
    public Category getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            UUID id = UUID.fromString(value);
            return categoryService.findById(id).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Category value) {
        if (value == null) {
            return "";
        }
        if (value.getId() != null) {
            return value.getId().toString();
        }
        return "";
    }
}
