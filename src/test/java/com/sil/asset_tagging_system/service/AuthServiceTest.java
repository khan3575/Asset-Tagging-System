package com.sil.asset_tagging_system.service;

import com.sil.asset_tagging_system.dto.request.LoginRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    AuthenticationManager manager;

    @InjectMocks
    AuthService service;

    @Test
    public void returnSuccessWhenValidToken()
    {
        LoginRequest loginRequest = new LoginRequest("sakib@gmail.com","Ab@/12bc");
        Authentication validAuthentication = new UsernamePasswordAuthenticationToken(loginRequest.email(),null, List.of());
        when(manager.authenticate(any())).thenReturn(validAuthentication);
        Authentication response = service.login(loginRequest);
        assertThat(response).isEqualTo(validAuthentication);
        assertThat(response.isAuthenticated()).isTrue();
    }

    @Test
    public void throwsWhenInvalidToken() throws Exception{
        LoginRequest loginRequest = new LoginRequest("sakib@gmail.com", "Ab@bcdbd");
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("Bad Credentials"));
        assertThatThrownBy(()-> service.login(loginRequest)).isInstanceOf(BadCredentialsException.class);
    }

}
