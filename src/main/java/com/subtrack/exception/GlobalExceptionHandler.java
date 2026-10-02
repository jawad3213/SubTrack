package com.subtrack.exception;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;


public class GlobalExceptionHandler extends ExceptionHandlerWrapper {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ExceptionHandler wrapped;

    public GlobalExceptionHandler(ExceptionHandler handler) {
        super(handler);
        this.wrapped = handler;
    }

    @Override
    public ExceptionHandler getWrapped() {
        return wrapped;
    }

    @Override
    public void handle() {
        for (Iterator<ExceptionQueuedEvent> i = getUnhandledExceptionQueuedEvents().iterator(); i.hasNext();) {
            ExceptionQueuedEvent event = i.next();
            ExceptionQueuedEventContext context = (ExceptionQueuedEventContext) event.getSource();
            Throwable cause = context.getException();

            try {
                logException(cause);
                addErrorMessage(cause);
            } finally {
                i.remove();
            }
        }
        getWrapped().handle();
    }

    private void logException(Throwable cause) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        LOGGER.error("Unexpected error", cause);
        LOGGER.warn("Global Exception Handler - Uncaught Exception: " + cause.getMessage());
        LOGGER.warn(sw.toString());
    }

    private void addErrorMessage(Throwable cause) {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null || fc.getResponseComplete()) {
            return;
        }

        // Only messages written for users are shown; everything else gets a generic text.
        String message = com.subtrack.util.FacesErrors.message(cause, "An unexpected error occurred. Please try again.");

        FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", message);
        fc.addMessage(null, facesMessage);
    }
}
