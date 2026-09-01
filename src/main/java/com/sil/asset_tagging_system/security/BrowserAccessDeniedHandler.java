package com.sil.asset_tagging_system.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.sil.asset_tagging_system.dto.Actor;
import com.sil.asset_tagging_system.model.enums.ActivityAction;
import com.sil.asset_tagging_system.model.enums.ActivityEntityType;
import com.sil.asset_tagging_system.service.AuditTrail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrowserAccessDeniedHandler implements AccessDeniedHandler {

    private final AuditTrail auditTrail;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        auditTrail.record(ActivityAction.ACCESS_DENIED, ActivityEntityType.AUTH)
            .by(new Actor(SecurityUtil.currentUserId(), SecurityUtil.primaryRole(), request.getRemoteAddr()))
            .refused(accessDeniedException.getMessage())
            .summary("Access denied to " + request.getRequestURI())
            .bestEffort()
            .save();

        response.sendRedirect(request.getContextPath() + "/dashboard?error");
    }
}
