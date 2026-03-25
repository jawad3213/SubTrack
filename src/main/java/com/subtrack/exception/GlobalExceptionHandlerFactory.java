package com.subtrack.exception;

import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerFactory;

public class GlobalExceptionHandlerFactory extends ExceptionHandlerFactory {

    private final ExceptionHandlerFactory parent;

    public GlobalExceptionHandlerFactory(ExceptionHandlerFactory parent) {
        super(parent);
        this.parent = parent;
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        return new GlobalExceptionHandler(parent.getExceptionHandler());
    }
}
