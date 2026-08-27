package com.sil.asset_tagging_system.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import com.sil.asset_tagging_system.security.RestAccessDeniedHandler;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Autowired
    private RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        //normal csrf
        // http.csrf(csrf -> csrf.disable())

        /* Note : if formLogin and httpBasic isnt mentioned into the filterchain be defaulted
        * unauthorized request are send 403 error code. that is for bidden
        *
        * here is added formLogin and httpBasic to ensure other developers find that we wanted
        * the unauthorized request return 403.
        * */
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(
                        auth -> auth
                                    // Must be the first matcher: FacesServlet no longer owns *.xhtml
                                    // once automatic-extensionless-mapping is on, so a direct .xhtml
                                    // request would otherwise fall through to the default servlet and
                                    // be served as raw Facelets source. /resources/** is the physical
                                    // webapp/resources folder (composite component sources included),
                                    // sealed for the same reason.
                                    .requestMatchers("/**/*.xhtml", "/*.xhtml").denyAll()
                                    .requestMatchers("/resources/**").denyAll()
                                    .requestMatchers("/login", "/css/**", "/js/**", "/jakarta.faces.resource/**").permitAll()
                                    .requestMatchers("/activity/**").hasRole("ADMIN")
                                    .anyRequest().authenticated()
                )
                .logout(
                        logout ->
                                logout.logoutUrl("/logout")
                                .logoutSuccessHandler((request, response, authentication)->{
                                    response.sendRedirect(request.getContextPath() + "/login?logout");})
                )
                .formLogin(
                    login -> login.loginPage("/login")
                    .defaultSuccessUrl("/dashboard?login=success",true)
                    .failureUrl("/login?error").permitAll()
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(handling-> handling.accessDeniedHandler(restAccessDeniedHandler)
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
