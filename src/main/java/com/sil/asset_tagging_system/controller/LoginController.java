package com.sil.asset_tagging_system.controller;

/*
 * Manual-trigger login, kept for reference/comparison against
 * CustomAuthenticationProcessingFilter (the active implementation).
 * Not a registered Spring bean while commented out.
 *
import com.sil.asset_tagging_system.dto.request.LoginRequest;
import com.sil.asset_tagging_system.dto.response.ApiResponse;
import com.sil.asset_tagging_system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController

@RequestMapping("/api/auth")
public class LoginController {
    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;
    @Autowired
    public LoginController(AuthService authService, SecurityContextRepository securityContextRepository)
    {
        this.authService = authService;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login")
    public ApiResponse<String> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response)
    {
        Authentication authentication = authService.login(loginRequest);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return new ApiResponse<String>(true, "hi succes", "hello world",LocalDateTime.now());
    }

        *
        *
        * This was the manuall implmementation of login feature. now that th CustomAuthenticationProcessingFilter
        * chain is setted up we commented out this whole class. dont need manually write login controller
        * filter handle it.
        *
        *
        *

}
*/
