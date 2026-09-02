package com.sil.asset_tagging_system.faces;

import java.io.IOException;
import java.util.Iterator;

import jakarta.faces.FacesException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;

import com.sil.asset_tagging_system.security.CorrelationFilter;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class CustomExceptionHandler extends ExceptionHandlerWrapper {

    public CustomExceptionHandler(ExceptionHandler wrapped) {
        super(wrapped);
    }

    @Override
    public void handle() throws FacesException {
        boolean redirected = false;

        for (Iterator<ExceptionQueuedEvent> it = getUnhandledExceptionQueuedEvents().iterator(); it.hasNext(); ) {
            ExceptionQueuedEvent event = it.next();
            Throwable cause = ((ExceptionQueuedEventContext) event.getSource()).getException();

            if (!(cause instanceof AbortProcessingException) && !redirected) {
                log.error("Unhandled exception reached CustomExceptionHandler (correlationId={})",
                        CorrelationFilter.getCurrentCorrelationId(), cause);

               FacesContext facesContext = FacesContext.getCurrentInstance();
                ExternalContext externalContext = facesContext.getExternalContext();
                try {
                    externalContext.redirect(externalContext.getRequestContextPath() + "/error");
                    redirected = true;
                } catch (IOException ioe) {
                    log.error("Failed to redirect to the error page", ioe);
                } finally {
                    facesContext.responseComplete();
                }
            }

            it.remove();
        }

        getWrapped().handle();
    }
}
