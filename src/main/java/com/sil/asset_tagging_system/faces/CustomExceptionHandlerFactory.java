package com.sil.asset_tagging_system.faces;

import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerFactory;


public class CustomExceptionHandlerFactory extends ExceptionHandlerFactory {

    

    public CustomExceptionHandlerFactory(ExceptionHandlerFactory parent) {
        super(parent);
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        return new CustomExceptionHandler(getWrapped().getExceptionHandler());
    }
}
