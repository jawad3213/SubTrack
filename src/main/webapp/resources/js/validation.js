// Client-side form validation for SubTrack
(function() {
    'use strict';

    window.SubTrackValidation = {
        validateSubscriptionForm: function(form) {
            var isValid = true;
            var errors = [];

            var name = form.querySelector('[id*="name"]');
            if (name && !name.value.trim()) {
                errors.push('Service name is required');
                isValid = false;
            }

            var price = form.querySelector('[id*="price"]');
            if (price) {
                var priceVal = parseFloat(price.value);
                if (isNaN(priceVal) || priceVal <= 0) {
                    errors.push('Price must be greater than 0');
                    isValid = false;
                }
            }

            var startDate = form.querySelector('[id*="startDate"]');
            if (startDate && startDate.value) {
                var date = new Date(startDate.value);
                var today = new Date();
                today.setHours(0, 0, 0, 0);
                if (date < today) {
                    errors.push('Start date cannot be in the past');
                    isValid = false;
                }
            }

            return { valid: isValid, errors: errors };
        },

        validateRegistration: function(form) {
            var isValid = true;
            var errors = [];

            var email = form.querySelector('[id*="email"]');
            if (email && email.value) {
                var emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                if (!emailRegex.test(email.value)) {
                    errors.push('Please enter a valid email address');
                    isValid = false;
                }
            }

            var password = form.querySelector('[id*="password"]');
            if (password) {
                if (password.value.length < 8) {
                    errors.push('Password must be at least 8 characters');
                    isValid = false;
                }
                var hasUpper = /[A-Z]/.test(password.value);
                var hasLower = /[a-z]/.test(password.value);
                var hasDigit = /[0-9]/.test(password.value);
                if (!hasUpper || !hasLower || !hasDigit) {
                    errors.push('Password must contain uppercase, lowercase, and numbers');
                    isValid = false;
                }
            }

            var confirmPassword = form.querySelector('[id*="confirmPassword"]');
            if (password && confirmPassword && password.value !== confirmPassword.value) {
                errors.push('Passwords do not match');
                isValid = false;
            }

            return { valid: isValid, errors: errors };
        },

        showErrors: function(errors, containerId) {
            var container = document.getElementById(containerId);
            if (!container) return;

            container.innerHTML = errors.map(function(err) {
                return '<div class="error-message">' + err + '</div>';
            }).join('');
        },

        initAutoTrim: function() {
            document.addEventListener('input', function(e) {
                if (e.target.tagName === 'INPUT' && e.target.type === 'text') {
                    e.target.value = e.target.value.trim();
                }
            });
        },

        formatCurrency: function(inputId) {
            var input = document.getElementById(inputId);
            if (!input) return;

            input.addEventListener('blur', function() {
                var val = parseFloat(this.value);
                if (!isNaN(val)) {
                    this.value = val.toFixed(2);
                }
            });
        }
    };

    document.addEventListener('DOMContentLoaded', function() {
        window.SubTrackValidation.initAutoTrim();
    });
})();