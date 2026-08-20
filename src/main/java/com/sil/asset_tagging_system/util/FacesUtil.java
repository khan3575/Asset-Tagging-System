package com.sil.asset_tagging_system.util;

import java.util.Map;

import jakarta.faces.context.FacesContext;

public final class FacesUtil {

    private FacesUtil() {
    }

    public static Map<String, String> getRequestParams() {
        return FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestParameterMap();
    }
}
