package com.sil.asset_tagging_system.bean;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sil.asset_tagging_system.security.CustomUserDetails;

@Named
@RequestScoped
public class HeaderBean {
    public String getFirstName(){
        CustomUserDetails userDetails = getCurrentUser();
        return userDetails != null ? userDetails.getFirstName() : "";
    }

    public String getRole()
    {
        CustomUserDetails userDetails = getCurrentUser();
        if(userDetails == null)
        {
            return null;
        }
        return userDetails.getAuthorities().stream().findFirst()
                .map(GrantedAuthority::getAuthority).orElse("");
    }


    private CustomUserDetails getCurrentUser()
    {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (principal instanceof CustomUserDetails userDetails) ? userDetails : null;
    }
}
