package com.sil.asset_tagging_system.util;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

public final class FacesMessages {

    private FacesMessages() {
    }

    public static void error(String message) {
        add(FacesMessage.SEVERITY_ERROR, message);
    }

    public static void warn(String message) {
        add(FacesMessage.SEVERITY_WARN, message);
    }

    public static void info(String message) {
        add(FacesMessage.SEVERITY_INFO, message);
    }

    private static void add(FacesMessage.Severity severity, String message) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, message, null));
    }
}
