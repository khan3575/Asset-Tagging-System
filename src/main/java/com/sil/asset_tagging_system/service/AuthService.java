package com.sil.asset_tagging_system.service;

import com.sil.asset_tagging_system.dto.request.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    AuthenticationManager manager;

    @Autowired
    AuthService(AuthenticationManager manager)
    {
        this.manager = manager;
    }

    public Authentication login(LoginRequest loginRequest)
    {
        UsernamePasswordAuthenticationToken token
                = new UsernamePasswordAuthenticationToken(loginRequest.email()
                        , loginRequest.password());
        return manager.authenticate(token);
    }
}
