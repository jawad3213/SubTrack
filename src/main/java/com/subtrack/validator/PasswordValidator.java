package com.subtrack.validator;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

@FacesValidator("passwordValidator")
public class PasswordValidator implements Validator {

    private static final int MIN_LENGTH = 8;

    @Override
    public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
        if (value == null || value.toString().trim().isEmpty()) {
            throw new ValidatorException(new FacesMessage(
                FacesMessage.SEVERITY_ERROR, 
                "Validation Error", 
                "Password is required"
            ));
        }

        String password = value.toString();
        
        if (password.length() < MIN_LENGTH) {
            throw new ValidatorException(new FacesMessage(
                FacesMessage.SEVERITY_ERROR, 
                "Validation Error", 
                "Password must be at least " + MIN_LENGTH + " characters"
            ));
        }

        boolean hasUpper = false, hasLower = false, hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isDigit(c)) hasDigit = true;
        }

        if (!hasUpper || !hasLower || !hasDigit) {
            throw new ValidatorException(new FacesMessage(
                FacesMessage.SEVERITY_ERROR, 
                "Validation Error", 
                "Password must contain uppercase, lowercase, and digits"
            ));
        }
    }
}
