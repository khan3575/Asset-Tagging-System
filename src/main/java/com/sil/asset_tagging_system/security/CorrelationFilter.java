package com.sil.asset_tagging_system.security;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationFilter implements Filter {
    
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException
    {
        UUID id = UUID.randomUUID();
        CURRENT.set(id);
        try{
            filterChain.doFilter(request, response);
        }
        finally{
            CURRENT.remove();
        }
    }

    public static UUID getCurrentCorrelationId()
    {
        return CURRENT.get();
    }
}
