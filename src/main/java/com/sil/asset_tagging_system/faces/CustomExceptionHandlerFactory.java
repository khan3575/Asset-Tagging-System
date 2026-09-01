package com.sil.asset_tagging_system.faces;

import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerFactory;

/**
 * Discovered via META-INF/services/jakarta.faces.context.ExceptionHandlerFactory.
 * Mojarra instantiates this with the previously-registered factory (the default one)
 * passed to the constructor, which is the standard way to decorate a JSF factory
 * without touching the default implementation.
 */
public class CustomExceptionHandlerFactory extends ExceptionHandlerFactory {

    private final ExceptionHandlerFactory parent;

    public CustomExceptionHandlerFactory(ExceptionHandlerFactory parent) {
        this.parent = parent;
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        return new CustomExceptionHandler(parent.getExceptionHandler());
    }
}
