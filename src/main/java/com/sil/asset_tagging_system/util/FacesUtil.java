package com.sil.asset_tagging_system.util;

import jakarta.faces.context.FacesContext;

import java.util.Map;

public final class FacesUtil {

    private FacesUtil() {
    }

    public static Map<String, String> getRequestParams() {
        return FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestParameterMap();
    }
}
