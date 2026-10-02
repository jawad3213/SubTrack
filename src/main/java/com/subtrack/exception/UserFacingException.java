package com.subtrack.exception;

/**
 * An error whose message was written to be shown to the end user (e.g. "Email already registered").
 * Any other exception is logged and replaced by a generic message, so internal details
 * (SQL, stack traces, remote API errors) never reach the UI.
 *
 * Extends IllegalArgumentException so existing callers that catch it keep working.
 */
public class UserFacingException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public UserFacingException(String message) {
        super(message);
    }
}
