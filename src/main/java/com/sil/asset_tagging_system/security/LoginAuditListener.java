package com.sil.asset_tagging_system.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LoginAuditListener {


    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event)
    {
        Object principal = event.getAuthentication().getPrincipal();
        Long userId = (principal instanceof CustomUserDetails userDetails) ? userDetails.getUserId(): null;
        String email = event.getAuthentication().getName();
        log.info("Login success for email : {} , user_id : {} ", email, userId);
        /*
            auditLogDao.log(userId, "LOGIN_SUCCESS", "AUTH", userId, "login : "+email, extractIp(event.getAuthentication()));

            replace altter
        
        */
   }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event)
    {
        String attemptedEmail = event.getAuthentication().getName();

        log.warn("Login failed for email : {}", attemptedEmail);
       // auditLogDao.log(null, "LOGIN_FAILURE", "AUTH", null, "Failed login attempt : "+attemptedEmail, extractIp(event.getAuthentication()));
    }

    public String extractIp(Authentication auth)
    {
        return (auth.getDetails() instanceof WebAuthenticationDetails webAuth) ? webAuth.getRemoteAddress() : "";
    }


}
