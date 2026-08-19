package com.sil.asset_tagging_system.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.sil.asset_tagging_system.dao.ActivityLogDao;
import com.sil.asset_tagging_system.model.ActivityLog;
import com.sil.asset_tagging_system.model.enums.ActivityAction;
import com.sil.asset_tagging_system.model.enums.ActivityEntityType;
import com.sil.asset_tagging_system.model.enums.ActivityOutcome;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.util.FacesUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Component
public class LoginAuditListener {
    private final ActivityLogDao activityLogDao;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event)
    {
        Object principal = event.getAuthentication().getPrincipal();
        Long userId = (principal instanceof CustomUserDetails userDetails) ? userDetails.getUserId(): null;
        String email = event.getAuthentication().getName();
        log.info("Login success for email : {} , user_id : {} ", email, userId);
        
       // activityLogDao.log(userId, "LOGIN_SUCCESS", "AUTH", userId, "login : "+email, extractIp(event.getAuthentication()));
        String roleName = event.getAuthentication().getAuthorities().stream()
                        .findFirst()
                        .map(GrantedAuthority::getAuthority)
                        .orElse(null);


        ActivityLog act = ActivityLog.builder()
            .correlationId(CorrelationFilter.CURRENT.get())
            .sequenceInAction((byte) 1)
            .ipAddress(FacesUtil.getRemoteAddress())
            .entityType(ActivityEntityType.AUTH)
            .action(ActivityAction.LOGIN_SUCCEEDED)
            .outcome(ActivityOutcome.SUCCEEDED)
            .actorUserId(userId)
            .actorRoles( roleName != null ? RoleName.valueOf(roleName) : null)
            .summary("Login succeeded for " + email)
            .build();
        activityLogDao.log(act);
   }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event)
    {
        String attemptedEmail = event.getAuthentication().getName();

        log.warn("Login failed for email : {}", attemptedEmail);
       // auditLogDao.log(null, "LOGIN_FAILURE", "AUTH", null, "Failed login attempt : "+attemptedEmail, extractIp(event.getAuthentication()));
    
        ActivityLog act = ActivityLog.builder()
            .correlationId(CorrelationFilter.CURRENT.get())
            .sequenceInAction((byte)1)
            .ipAddress(FacesUtil.getRemoteAddress())
            .entityType(ActivityEntityType.AUTH)
            .action(ActivityAction.LOGIN_FAILED)
            .failureReason(event.getException().getMessage())
            .outcome(ActivityOutcome.FAILED)
            .summary("Login failed attempted email - " + attemptedEmail)
            .build();
        activityLogDao.log(act);
    }

    


}
