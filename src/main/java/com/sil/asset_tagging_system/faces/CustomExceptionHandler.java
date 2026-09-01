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

/**
 * Safety net for exceptions that escape a bean action uncaught -- bugs, DB hiccups,
 * anything the calling bean didn't already catch as a BusinessRuleException. Expected
 * validation failures are still handled locally in the beans (catch + FacesMessages +
 * "return null" to redisplay the same form) -- this handler never sees those.
 *
 * Logs the exception with the request's correlation id, then redirects (not forwards --
 * see the FORWARD-dispatch note below) to a plain "something went wrong" page instead of
 * letting a stack trace reach the browser.
 */
@Slf4j
public class CustomExceptionHandler extends ExceptionHandlerWrapper {

    private final ExceptionHandler wrapped;

    public CustomExceptionHandler(ExceptionHandler wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public ExceptionHandler getWrapped() {
        return wrapped;
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

                // A forward here would re-enter Spring Security with the forward's *target*
                // path (webapp/error.xhtml), which the SecurityConfig denyAll rule on
                // "/**/*.xhtml" would then reject -- see docs/development-plan.md's T8.3 note
                // on FORWARD-dispatch re-filtering. A redirect goes through the extensionless
                // "/error" mapping instead, same as every other navigation in this app.
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
