package com.sil.asset_tagging_system.security;


import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

import com.sil.asset_tagging_system.model.User;


public class CustomUserDetails implements UserDetails {

    private final User user;


    public CustomUserDetails(User user)
    {
        this.user = user;
    }

    public Long getUserId() { return user.getId(); }
    public String getFirstName() { return user.getFirstName(); }
    public String getLastName() { return user.getLastName(); }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toSet());
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return user.getEnabled();
    }
}
