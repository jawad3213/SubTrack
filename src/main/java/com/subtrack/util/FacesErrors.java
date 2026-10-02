package com.subtrack.util;

import com.subtrack.exception.UserFacingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Turns exceptions into messages that are safe to show in the UI. */
public final class FacesErrors {

    private static final Logger LOGGER = LoggerFactory.getLogger(FacesErrors.class);

    public static final String GENERIC_MESSAGE = "Something went wrong. Please try again.";

    private FacesErrors() {
    }

    /**
     * Returns the exception's own message if it is a {@link UserFacingException} (anywhere in the
     * cause chain); otherwise logs the full exception and returns {@code fallback}.
     */
    public static String message(Throwable e, String fallback) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof UserFacingException) {
                return t.getMessage();
            }
        }
        LOGGER.error(fallback, e);
        return fallback;
    }

    public static String message(Throwable e) {
        return message(e, GENERIC_MESSAGE);
    }
}
