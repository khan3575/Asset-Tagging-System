package com.sil.asset_tagging_system.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

import com.sil.asset_tagging_system.dao.AuditLogDao;

@Component
public class LoginAuditListener {
    private static final Logger log = LoggerFactory.getLogger(LoginAuditListener.class);

    private final AuditLogDao auditLogDao;

    @Autowired
    LoginAuditListener(AuditLogDao auditLogDao)
    {
        this.auditLogDao = auditLogDao;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event)
    {
        Object principal = event.getAuthentication().getPrincipal();
        Long userId = (principal instanceof CustomUserDetails userDetails) ? userDetails.getUserId(): null;
        String email = event.getAuthentication().getName();
        log.info("Login success for email : {} , user_id : {} ", email, userId);
        auditLogDao.log(userId, "LOGIN_SUCCESS", "AUTH", userId, "login : "+email, extractIp(event.getAuthentication()));
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event)
    {
        String attemptedEmail = event.getAuthentication().getName();

        log.warn("Login failed for email : {}", attemptedEmail);
        auditLogDao.log(null, "LOGIN_FAILURE", "AUTH", null, "Failed login attempt : "+attemptedEmail, extractIp(event.getAuthentication()));
    }

    public String extractIp(Authentication auth)
    {
        return (auth.getDetails() instanceof WebAuthenticationDetails webAuth) ? webAuth.getRemoteAddress() : "";
    }


}
