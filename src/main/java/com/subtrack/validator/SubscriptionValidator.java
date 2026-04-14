package com.subtrack.validator;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;
import java.math.BigDecimal;
import java.time.LocalDate;

@FacesValidator("subscriptionValidator")
public class SubscriptionValidator implements Validator {

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (value == null) {
            throw new ValidatorException(new FacesMessage(
                FacesMessage.SEVERITY_ERROR, "Validation Error", "Value is required"));
        }

        String fieldId = component.getId();

        if (fieldId != null && fieldId.contains("price")) {
            validatePrice(context, value);
        } else if (fieldId != null && fieldId.contains("startDate")) {
            validateStartDate(context, value);
        } else if (fieldId != null && fieldId.contains("name")) {
            validateName(context, value);
        }
    }

    private void validatePrice(FacesContext context, Object value) throws ValidatorException {
        BigDecimal price;
        
        if (value instanceof BigDecimal) {
            price = (BigDecimal) value;
        } else if (value instanceof String) {
            try {
                price = new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                throw new ValidatorException(new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Error", "Invalid price format"));
            }
        } else {
            throw new ValidatorException(new FacesMessage(
                FacesMessage.SEVERITY_ERROR, "Error", "Price must be a number"));
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidatorException(new FacesMessage(
                FacesMessage.SEVERITY_ERROR, "Error", "Price must be greater than 0"));
        }

        if (price.compareTo(new BigDecimal("10000")) > 0) {
            throw new ValidatorException(new FacesMessage(
                FacesMessage.SEVERITY_ERROR, "Error", "Price seems unusually high"));
        }
    }

    private void validateStartDate(FacesContext context, Object value) throws ValidatorException {
        LocalDate date;
        
        if (value instanceof LocalDate) {
            date = (LocalDate) value;
        } else if (value instanceof String) {
            try {
                date = LocalDate.parse((String) value);
            } catch (Exception e) {
                throw new ValidatorException(new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Error", "Invalid date format"));
            }
        } else {
            return;
        }

        LocalDate yesterday = LocalDate.now().minusDays(1);
        if (date.isBefore(yesterday)) {
            throw new ValidatorException(new FacesMessage(
                FacesMessage.SEVERITY_ERROR, "Error", "Start date cannot be in the past"));
        }
    }

    private void validateName(FacesContext context, Object value) throws ValidatorException {
        String name = value instanceof String ? (String) value : value.toString();
        
        if (name == null || name.trim().isEmpty()) {
            throw new ValidatorException(new FacesMessage(
                FacesMessage.SEVERITY_ERROR, "Error", "Service name is required"));
        }

        if (name.length() > 255) {
            throw new ValidatorException(new FacesMessage(
                FacesMessage.SEVERITY_ERROR, "Error", "Service name too long"));
        }
    }
}