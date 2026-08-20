package com.sil.asset_tagging_system.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.NoArgsConstructor;
@NoArgsConstructor
public final class SecurityUtil {

    public static Optional<CustomUserDetails> currentUser()
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
        {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof CustomUserDetails userDetails)
        {
            return Optional.of(userDetails);
        }
        return Optional.empty();
    }

    public static Long currentUserId()
    {
        return currentUser().map(CustomUserDetails::getUserId).orElse(null);
    }

    public static String primaryRole()
    {
        return SecurityUtil.primaryRole(SecurityContextHolder.getContext().getAuthentication());
    }
    
    public static String primaryRole(Authentication auth)
    {
        if (auth == null)
        {
            return null;
        }
        return auth.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse(null);
    }
}
