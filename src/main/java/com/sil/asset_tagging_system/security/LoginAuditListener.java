package com.sil.asset_tagging_system.security;

import com.sil.asset_tagging_system.dao.AuditLogDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Component;

@Component
public class LoginAuditListener {

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

        auditLogDao.log(userId, "LOGIN_SUCCESS", "AUTH", userId, "login : "+email, null);
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event)
    {
        String attemptedEmail = event.getAuthentication().getName();

        auditLogDao.log(null, "LOGIN_FAILURE", "AUTH", null, "Failed login attempt : "+attemptedEmail, null);
    }

}
