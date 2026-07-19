package com.sil.asset_tagging_system.controller;

import com.sil.asset_tagging_system.dto.request.LoginRequest;
import com.sil.asset_tagging_system.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
public class LoginControllerTest {

    @Autowired
    MockMvc mockMvc;


    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    SecurityContextRepository securityContextRepository;


    @Test
    public void validLoginRequest() throws Exception
    {
        LoginRequest loginRequest = new LoginRequest("sakib@gmail.com", "aAV12@23/");
        Authentication validAuth = new UsernamePasswordAuthenticationToken("sakib@gmail.com", "aAV12@23", List.of());
        when(authService.login(any(LoginRequest.class))).thenReturn(validAuth);

        String requestJson = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void invalidLoginRequestReturnUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest("sakibkhan@gmail.com", "a@b1CVdd/");
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("Bad Credentials"));
        String requestJson = objectMapper.writeValueAsString(loginRequest);
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        ).andExpect(status().isUnauthorized());
    }
    @Test
    public void sessionPersist() throws Exception{
        LoginRequest loginRequest = new LoginRequest("sakib@gmail.com", "a2@bAaaa");
        Authentication expectedAuth = new UsernamePasswordAuthenticationToken("sakib@gmail.com", null, List.of(()->"ROLE_USER"));

        when(authService.login(any(LoginRequest.class))).thenReturn(expectedAuth);
        String requestJson = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        ).andExpect(status().isOk());

        // url execute hoilo akhon check korbo je context set hoiche ki na
        ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);
        verify(securityContextRepository, times(1)).saveContext(contextCaptor.capture(),any(),any());

        SecurityContext saveContext = contextCaptor.getValue();
        assertThat(saveContext.getAuthentication()).isEqualTo(expectedAuth);
    }
    @Test
    public void invalidPayloadReturnsBadRequest() throws Exception{
        LoginRequest loginRequest = new LoginRequest("sakib@gmail.com", "alllowercase1");
        String requestJson = objectMapper.writeValueAsString(loginRequest);


        mockMvc.perform(post("/api/auth/login").
                contentType(MediaType.APPLICATION_JSON).content(requestJson)).andExpect(status().isBadRequest());
    }


}