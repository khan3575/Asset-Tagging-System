package com.sil.asset_tagging_system.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import com.sil.asset_tagging_system.dao.ActivityLogDao;
import com.sil.asset_tagging_system.model.ActivityLog;
import com.sil.asset_tagging_system.model.enums.ActivityAction;
import com.sil.asset_tagging_system.model.enums.ActivityEntityType;
import com.sil.asset_tagging_system.model.enums.ActivityOutcome;
import com.sil.asset_tagging_system.model.enums.RoleName;
import com.sil.asset_tagging_system.util.WebUtil;

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
        String roleName = SecurityUtil.primaryRole(event.getAuthentication());


        ActivityLog act = ActivityLog.builder()
            .correlationId(CorrelationFilter.getCurrentCorrelationId())
            .sequenceInAction((short) 1)
            .ipAddress(WebUtil.getRemoteAddress(event.getAuthentication()))
            .entityType(ActivityEntityType.AUTH)
            .action(ActivityAction.LOGIN_SUCCEEDED)
            .outcome(ActivityOutcome.SUCCEEDED)
            .actorUserId(userId)
            .actorRoles( roleName != null ? RoleName.valueOf(roleName) : null)
            .summary("Login succeeded for " + email)
            .build();

        try{
            activityLogDao.log(act);
        }
        catch(Exception e)
        {
            log.error("Failed to write LOGIN_SUCCEEDED activity log for user {}",userId, e);
        }
   }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event)
    {
        String attemptedEmail = event.getAuthentication().getName();

        log.warn("Login failed for email : {}", attemptedEmail);
       // auditLogDao.log(null, "LOGIN_FAILURE", "AUTH", null, "Failed login attempt : "+attemptedEmail, extractIp(event.getAuthentication()));
    
        ActivityLog act = ActivityLog.builder()
            .correlationId(CorrelationFilter.getCurrentCorrelationId())
            .sequenceInAction((short) 1)
            .ipAddress(WebUtil.getRemoteAddress(event.getAuthentication()))
            .entityType(ActivityEntityType.AUTH)
            .action(ActivityAction.LOGIN_FAILED)
            .failureReason(event.getException().getMessage())
            .outcome(ActivityOutcome.FAILED)
            .summary("Login failed attempted email - " + attemptedEmail)
            .build();
        try{
            activityLogDao.log(act);
        }
        catch (Exception e) {
            log.error("Failed to write LOGIN_FAILED activity log for attempted email {}", attemptedEmail, e);
        }
    }

    


}
