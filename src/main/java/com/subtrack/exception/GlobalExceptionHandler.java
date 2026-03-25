package com.subtrack.exception;

import jakarta.faces.FacesException;
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
        cause.printStackTrace(pw);
        System.err.println("Global Exception Handler - Uncaught Exception: " + cause.getMessage());
        System.err.println(sw.toString());
    }

    private void addErrorMessage(Throwable cause) {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null || fc.getResponseComplete()) {
            return;
        }

        String message = "An unexpected error occurred. Please try again.";
        
        if (cause instanceof IllegalArgumentException) {
            message = cause.getMessage();
        } else if (cause instanceof IllegalStateException) {
            message = cause.getMessage();
        }

        FacesMessage facesMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", message);
        fc.addMessage(null, facesMessage);
    }
}
