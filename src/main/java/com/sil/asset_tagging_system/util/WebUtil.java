package com.sil.asset_tagging_system.util;

import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

public final class WebUtil {

    private WebUtil() {
    }

    public static String getRemoteAddress(Authentication auth) {
        if (auth == null) {
            return null;
        }
        if (auth.getDetails() instanceof WebAuthenticationDetails details) {
            return details.getRemoteAddress();
        }
        return null;
    }

    public static String getRemoteAddress()
    {
        return ((HttpServletRequest) FacesContext.getCurrentInstance()
            .getExternalContext().getRequest()).getRemoteAddr();
    }
}
