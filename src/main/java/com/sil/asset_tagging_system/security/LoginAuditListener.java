package com.sil.asset_tagging_system.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.sil.asset_tagging_system.dto.Actor;
import com.sil.asset_tagging_system.model.enums.ActivityAction;
import com.sil.asset_tagging_system.model.enums.ActivityEntityType;
import com.sil.asset_tagging_system.service.AuditTrail;
import com.sil.asset_tagging_system.util.WebUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Component
public class LoginAuditListener {

    private final AuditTrail auditTrail;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event)
    {
        Object principal = event.getAuthentication().getPrincipal();
        Long userId = (principal instanceof CustomUserDetails userDetails) ? userDetails.getUserId(): null;
        String email = event.getAuthentication().getName();
        log.info("Login success for email : {} , user_id : {} ", email, userId);

        auditTrail.record(ActivityAction.LOGIN_SUCCEEDED, ActivityEntityType.AUTH)
            .by(new Actor(userId, SecurityUtil.primaryRole(event.getAuthentication()),
                          WebUtil.getRemoteAddress(event.getAuthentication())))
            .summary("Login succeeded for " + email)
            .bestEffort()
            .save();
   }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event)
    {
        String attemptedEmail = event.getAuthentication().getName();
        log.warn("Login failed for email : {}", attemptedEmail);
        
        auditTrail.record(ActivityAction.LOGIN_FAILED, ActivityEntityType.AUTH)
            .by(new Actor(null, null, WebUtil.getRemoteAddress(event.getAuthentication())))
            .failed(event.getException().getMessage())
            .summary("Login failed attempted email - " + attemptedEmail)
            .bestEffort()
            .save();
    }
    
    public void recordLogout(Authentication authentication, String ipAddress)
    {
        Object principal = authentication.getPrincipal();
        Long userId = (principal instanceof CustomUserDetails userDetails) ? userDetails.getUserId() : null;

        auditTrail.record(ActivityAction.LOGOUT, ActivityEntityType.AUTH)
            .by(new Actor(userId, SecurityUtil.primaryRole(authentication), ipAddress))
            .summary("Logout for " + authentication.getName())
            .bestEffort()
            .save();
    }
}
