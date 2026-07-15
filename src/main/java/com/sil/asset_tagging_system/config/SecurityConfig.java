package com.sil.asset_tagging_system.config;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
    {
        //normal csrf
        // http.csrf(csrf -> csrf.disable())

        /* Note : if formLogin and httpBasic isnt mentioned into the filterchain be defaulted
        * unauthorized request are send 403 error code. that is for bidden
        *
        * here is added formLogin and httpBasic to ensure other developers find that we wanted
        * the unauthorized request return 403.
        * */
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(
                        // add more path inside when needed
                        auth -> auth.requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/api/employee/**").hasRole("EMPLOYEE")
                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                                .requestMatchers("/", "/*.html").permitAll()
                                .anyRequest().authenticated()
                ).sessionManagement(
                        session -> session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                .sessionFixation().migrateSession()
                                .maximumSessions(1)
                                .maxSessionsPreventsLogin(false)
                        )
                .build();



        /* Session Management related config and there properties
        *   1. sessionFixation().migrateSession() on successful login, Spring Security issues a
        *       new session ID and copies existing session attributes over, instead of reusing
        *       whatever session ID existed pre-login. This defeats session fixation attacks,
        *       where an
        *
        *       attacker tricks a victim into authenticating under a session ID the attacker
        *       already knows, then hijacks it post-login.
        *
        *   2. maximumSessions(1) — caps each user to one active session at a time.
        *
        *   3. maxSessionsPreventsLogin(false) — when a user logs in again elsewhere, their
        *       older session gets invalidated (the new login wins). Setting this true instead
        *       would block the new login while the old session is still alive — that's a valid
        *       alternative policy, worth deciding deliberately rather than defaulting.
        *
        *
        *   4. One dependency this needs: maximumSessions relies on Spring tracking when
        *       sessions actually get destroyed (logout, timeout). Without a registered
        *       HttpSessionEventPublisher bean, the session registry can lose track of dead
        *       sessions and undercount. Add:

            @Bean
            public HttpSessionEventPublisher httpSessionEventPublisher() {
                return new HttpSessionEventPublisher();
            }
        *
        *  */

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
    // needs to add cors for the front end or not sure how does manual origin is handled...

}
