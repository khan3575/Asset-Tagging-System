package com.sil.asset_tagging_system.util;

import java.util.Map;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

public final class FacesUtil {

    private FacesUtil() {
    }

    public static Map<String, String> getRequestParams() {
        return FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestParameterMap();
    }

    public static String getRemoteAddress(){
        return ((HttpServletRequest)FacesContext.getCurrentInstance()
            .getExternalContext()
            .getRequest())
            .getRemoteAddr();
    }
}
