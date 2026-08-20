package com.sil.asset_tagging_system.bean;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import com.sil.asset_tagging_system.security.CustomUserDetails;
import com.sil.asset_tagging_system.security.SecurityUtil;

@Named
@RequestScoped
public class HeaderBean {
    public String getFirstName(){
        return SecurityUtil.currentUser()
                .map(CustomUserDetails::getFirstName)
                .orElse("");
    }

    public String getRole()
    {
        return SecurityUtil.primaryRole();
    }
}
