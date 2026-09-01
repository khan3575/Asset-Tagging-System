package com.sil.asset_tagging_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import com.sil.asset_tagging_system.security.BrowserAccessDeniedHandler;
import com.sil.asset_tagging_system.security.LoginAuditListener;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    private final BrowserAccessDeniedHandler browserAccessDeniedHandler;
    private final LoginAuditListener loginAuditListener;

    public SecurityConfig(@Lazy BrowserAccessDeniedHandler browserAccessDeniedHandler,
                          @Lazy LoginAuditListener loginAuditListener)
    {
        this.browserAccessDeniedHandler = browserAccessDeniedHandler;
        this.loginAuditListener = loginAuditListener;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        http.cors(Customizer.withDefaults())
                .authorizeHttpRequests(
                        auth -> auth
                                    
                                    .requestMatchers("/**/*.xhtml", "/*.xhtml").denyAll()
                                    .requestMatchers("/resources/**").denyAll()
                                    .requestMatchers("/login", "/forgot-password", "/css/**", "/js/**", "/jakarta.faces.resource/**").permitAll()
                                    .requestMatchers("/user/form").hasRole("ADMIN")
                                    .requestMatchers("/asset/form").hasRole("ADMIN")
                                    .requestMatchers("/activity/**").hasRole("ADMIN")
                                    .requestMatchers("/approval/**").hasRole("ADMIN")
                                    .anyRequest().authenticated()
                )
                .logout(
                        logout ->
                                logout.logoutUrl("/logout")
                                .logoutSuccessHandler((request, response, authentication)->{
                                    // added null check for the authentication
                                    if (authentication != null) {
                                        loginAuditListener.recordLogout(authentication, request.getRemoteAddr());
                                    }
                                    response.sendRedirect(request.getContextPath() + "/login?logout");})
                )
                .formLogin(
                    login -> login.loginPage("/login")
                    .defaultSuccessUrl("/dashboard?login=success",true)
                    .failureUrl("/login?error").permitAll()
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(handling-> handling.accessDeniedHandler(browserAccessDeniedHandler)
                )
                .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                .sessionFixation().migrateSession()
                                .maximumSessions(1)
                                .maxSessionsPreventsLogin(false)
                        );
        return http.build();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityContextRepository securityContextRepository(){
        return new HttpSessionSecurityContextRepository();
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
    {
        return config.getAuthenticationManager();
    }
}
