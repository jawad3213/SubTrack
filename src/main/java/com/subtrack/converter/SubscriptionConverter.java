package com.subtrack.converter;

import com.subtrack.entity.Subscription;
import com.subtrack.service.SubscriptionService;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;
import java.util.UUID;

@FacesConverter(value = "subscriptionConverter", managed = true)
public class SubscriptionConverter implements Converter<Subscription> {

    @Inject
    private SubscriptionService subscriptionService;

    @Override
    public Subscription getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            UUID id = UUID.fromString(value);
            return subscriptionService.findById(id).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Subscription value) {
        if (value == null) {
            return "";
        }
        if (value.getId() != null) {
            return value.getId().toString();
        }
        return "";
    }
}
