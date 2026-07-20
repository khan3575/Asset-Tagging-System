package com.sil.asset_tagging_system.config;

import com.sil.asset_tagging_system.dto.request.LoginRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
public class CustomAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {


    public CustomAuthenticationProcessingFilter(String defaultFilterProcessesUrl, AuthenticationManager authenticationManager) {
        super(defaultFilterProcessesUrl, authenticationManager);
    }

    @Override
    public @Nullable Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        ObjectMapper mapper = new ObjectMapper();
        LoginRequest loginRequest = mapper.readValue(request.getInputStream(), LoginRequest.class);

        String email = loginRequest.email();
        String password = loginRequest.password();
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(email, password);
        return this.getAuthenticationManager().authenticate(token);
    }
}
